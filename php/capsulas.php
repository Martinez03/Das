<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Headers: Content-Type");
header("Access-Control-Allow-Methods: POST, GET, OPTIONS");
header("Content-Type: application/json");

ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

$servername = "localhost";
$username = "Xpmartinez084";
$password = "jFUVCzQwKz";
$dbname = "Xpmartinez084_geocapsule_db";

$conn = new mysqli($servername, $username, $password, $dbname);
if ($conn->connect_error) {
    die(json_encode(["success" => false, "message" => "Conexión fallida"]));
}

$action = $_GET['action'] ?? '';

switch ($action) {

    // 1. Crear cápsula con imágenes
    case 'crear_con_imagenes':
        $titulo = $_POST['titulo'] ?? '';
        $descripcion = $_POST['descripcion'] ?? '';
        $latitud = (float)($_POST['latitud'] ?? 0);
        $longitud = (float)($_POST['longitud'] ?? 0);
        $usuario_id = (int)($_POST['usuario_id'] ?? 0);
    
        try {
            $conn->autocommit(FALSE); // Iniciar transacción
    
            // Insertar cápsula
            $stmt = $conn->prepare("INSERT INTO capsulas (usuario_id, titulo, descripcion, latitud, longitud) VALUES (?, ?, ?, ?, ?)");
            $stmt->bind_param("issdd", $usuario_id, $titulo, $descripcion, $latitud, $longitud);
            
            if (!$stmt->execute()) {
                throw new Exception("Error insertando cápsula: " . $stmt->error);
            }
            
            $capsula_id = $conn->insert_id;
            $imagenesSubidas = [];
    
            // Procesar imágenes
            if (!empty($_POST['imagen']) && is_array($_POST['imagen'])) {
                $stmt_img = $conn->prepare("INSERT INTO imagenes (capsula_id, foto) VALUES (?, ?)");
                if (!$stmt_img) {
                    throw new Exception("Error preparando query de imágenes: " . $conn->error);
                }
    
                foreach ($_POST['imagen'] as $imagenBase64) {
                    $fotoBinaria = base64_decode($imagenBase64);
                    if ($fotoBinaria === false) {
                        error_log("Base64 inválido");
                        continue;
                    }
    
                    $null = NULL;
                    $stmt_img->bind_param("ib", $capsula_id, $null);
                    $stmt_img->send_long_data(1, $fotoBinaria); // Manejo correcto de BLOB
    
                    if (!$stmt_img->execute()) {
                        error_log("Error insertando imagen: " . $stmt_img->error);
                        continue;
                    }
                    
                    $imagenesSubidas[] = $conn->insert_id;
                }
                $stmt_img->close();
            }
    
            $conn->commit();
            echo json_encode([
                "success" => true,
                "capsula_id" => $capsula_id,
                "imagenes" => $imagenesSubidas
            ]);
    
        } catch (Exception $e) {
            $conn->rollback();
            error_log($e->getMessage());
            http_response_code(500);
            echo json_encode([
                "success" => false,
                "message" => "Error interno del servidor: " . $e->getMessage()
            ]);
        } finally {
            $conn->autocommit(TRUE);
        }
        break;
    // 2. Listar cápsulas por usuario
    case 'listar_por_usuario':
        $usuario_id = (int)($_GET['usuario_id'] ?? 0);
        
        try {
            // Obtener cápsulas del usuario
            $stmt = $conn->prepare("SELECT * FROM capsulas WHERE usuario_id = ?");
            $stmt->bind_param("i", $usuario_id);
            $stmt->execute();
            $result = $stmt->get_result();
    
            $capsulas = [];
    
            while ($row = $result->fetch_assoc()) {
                $capsula_id = $row['id'];
    
                // Obtener imágenes como BLOB convertidas a Base64
                $img_stmt = $conn->prepare("SELECT id, foto FROM imagenes WHERE capsula_id = ?");
                $img_stmt->bind_param("i", $capsula_id);
                $img_stmt->execute();
                $img_result = $img_stmt->get_result();
    
                $imagenes = [];
                while ($img_row = $img_result->fetch_assoc()) {
                    // Convertir BLOB a Base64
                    $imagenBase64 = base64_encode($img_row['foto']);
                    $imagenes[] = [
                        'id' => $img_row['id'],
                        'imagen' => $imagenBase64 // Envía el BLOB como Base64
                    ];
                }
    
                $row['imagenes'] = $imagenes;
                $capsulas[] = $row;
            }
    
            echo json_encode([
                "success" => true,
                "capsulas" => $capsulas
            ]);
    
        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode([
                "success" => false,
                "message" => "Error al listar cápsulas: " . $e->getMessage()
            ]);
        }
        break;

    // 3. Eliminar cápsula (y sus imágenes)
    case 'eliminar_capsula':
        $capsula_id = (int)($_POST['capsula_id'] ?? 0);
        try {
            $conn->autocommit(FALSE); // Iniciar transacción
    
            // 1. Eliminar imágenes asociadas (si existen)
            $delete_images = $conn->prepare("DELETE FROM imagenes WHERE capsula_id = ?");
            $delete_images->bind_param("i", $capsula_id);
            $delete_images->execute();
            
            if ($delete_images->affected_rows === -1) {
                throw new Exception("Error eliminando imágenes: " . $delete_images->error);
            }
    
            // 2. Eliminar la cápsula
            $delete_capsule = $conn->prepare("DELETE FROM capsulas WHERE id = ?");
            $delete_capsule->bind_param("i", $capsula_id);
            $delete_capsule->execute();
            
            if ($delete_capsule->affected_rows === 0) {
                throw new Exception("No se encontró la cápsula con ID: $capsula_id");
            }
    
            $conn->commit();
            
            echo json_encode([
                "success" => true,
                "message" => "Cápsula y sus imágenes eliminadas correctamente",
                "deleted_rows" => $delete_capsule->affected_rows
            ]);
    
        } catch (Exception $e) {
            $conn->rollback();
            http_response_code(500);
            echo json_encode([
                "success" => false,
                "message" => $e->getMessage(),
                "error_details" => $conn->error
            ]);
        } finally {
            $conn->autocommit(TRUE);
        }
        break;

    // 4. Editar cápsula (solo título y descripción)
    case 'editar_capsula':
        $capsula_id = $_POST['capsula_id'] ?? 0;
        $titulo = $_POST['titulo'] ?? '';
        $descripcion = $_POST['descripcion'] ?? '';

        $stmt = $conn->prepare("UPDATE capsulas SET titulo = ?, descripcion = ? WHERE id = ?");
        $stmt->bind_param("ssi", $titulo, $descripcion, $capsula_id);

        if ($stmt->execute()) {
            echo json_encode(["success" => true, "message" => "Cápsula actualizada"]);
        } else {
            echo json_encode(["success" => false, "message" => "Error al actualizar"]);
        }
        break;

    default:
        echo json_encode(["success" => false, "message" => "Acción no válida"]);
        break;
}

$conn->close();
?>
