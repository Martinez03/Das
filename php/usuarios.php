<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

header("Content-Type: application/json");
$servername = "localhost";
$username = "Xpmartinez084";
$password = "jFUVCzQwKz";
$dbname = "Xpmartinez084_geocapsule_db";

$conn = new mysqli($servername, $username, $password, $dbname);
if ($conn->connect_error) {
    die(json_encode(["error" => "Conexión fallida: " . $conn->connect_error]));
}

$action = $_GET['action'] ?? '';

switch ($action) {
    case 'register':
        $nombre = $_POST['nombre'];
        $correo = $_POST['correo'];
        $contrasena = password_hash($_POST['contrasena'], PASSWORD_BCRYPT);

        $stmt = $conn->prepare("INSERT INTO usuarios (nombre, correo, contrasena) VALUES (?, ?, ?)");
        $stmt->bind_param("sss", $nombre, $correo, $contrasena);

        if ($stmt->execute()) {
            echo json_encode(["success" => true, "message" => "Usuario registrado"]);
        } else {
            echo json_encode(["success" => false, "message" => "Error al registrar"]);
        }
        break;

    case 'login':
        $correo = $_POST['correo'];
        $contrasena = $_POST['contrasena'];
    
        $stmt = $conn->prepare("SELECT id, nombre, contrasena FROM usuarios WHERE correo = ?");
        $stmt->bind_param("s", $correo);
        $stmt->execute();
        $result = $stmt->get_result();
        
        if ($result->num_rows === 0) {
            echo json_encode([
                "success" => false,
                "message" => "Usuario no encontrado"
            ]);
            exit;
        }
    
        $user = $result->fetch_assoc();
        
        if (!password_verify($contrasena, $user['contrasena'])) {
            echo json_encode([
                "success" => false,
                "message" => "Contraseña incorrecta"
            ]);
            exit;
        }
    
        // Respuesta CORRECTA con el formato que espera Android
        echo json_encode([
            "success" => true,
            "usuario" => [  // <-- Este es el campo que tu app espera
                "id" => $user['id'],
                "nombre" => $user['nombre']
            ]
        ]);
        break;

    default:
        echo json_encode(["error" => "Acción no válida"]);
}

$conn->close();
?>
