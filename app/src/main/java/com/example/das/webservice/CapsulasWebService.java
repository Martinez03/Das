package com.example.das.webservice;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.example.das.data.entity.Capsula;
import com.example.das.data.entity.Imagen;
import com.example.das.data.entity.ImagenCapsulaRelation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CapsulasWebService {

    private static final String BASE_URL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/pmartinez084/WEB/capsulas.php";
    private static final String TAG = "CapsulasWebService";

    public interface CapsulasCallback {
        void onSuccess(List<ImagenCapsulaRelation> capsulasConImagenes);
        void onSuccess(int capsulaId);
        void onError(String mensaje);
    }

    // Crear cápsula con imágenes
    public static void crearCapsula(Context context, int usuarioId,
                                    List<Uri> imagenesUris, String titulo, String descripcion,
                                    String latitud, String longitud, CapsulasCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String boundary = "*****";
                Uri.Builder uriBuilder = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter("action", "crear_con_imagenes");

                URL url = new URL(uriBuilder.build().toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (OutputStream outputStream = connection.getOutputStream();
                     DataOutputStream dataOutputStream = new DataOutputStream(outputStream)) {

                    // Parámetros básicos
                    writeMultipartField(dataOutputStream, boundary, "titulo", titulo);
                    writeMultipartField(dataOutputStream, boundary, "descripcion", descripcion);
                    writeMultipartField(dataOutputStream, boundary, "latitud", latitud);
                    writeMultipartField(dataOutputStream, boundary, "longitud", longitud);
                    writeMultipartField(dataOutputStream, boundary, "usuario_id", String.valueOf(usuarioId));

                    // Subir imágenes como Base64
                    if (imagenesUris != null && !imagenesUris.isEmpty()) {
                        int i = 0;
                        for (Uri uri : imagenesUris) {
                            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                                // Convertir a Bitmap y luego a Base64
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                String fotoBase64 = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);

                                // Campos para cada imagen
                                writeMultipartField(dataOutputStream, boundary, "imagen[]", fotoBase64);
                                i++;
                            } catch (Exception e) {
                                Log.e(TAG, "Error procesando imagen: " + e.getMessage());
                            }
                        }
                    }

                    dataOutputStream.writeBytes("--" + boundary + "--\r\n");
                }

                handleResponse(connection, response -> {
                    // Manejo de respuesta (igual que antes)
                }, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error crear cápsula: " + e.getMessage());
                callback.onError("Error de conexión");
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    // Obtener cápsulas con imágenes
    public static void obtenerCapsulasPorUsuario(int usuarioId, CapsulasCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                Uri.Builder uriBuilder = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter("action", "listar_por_usuario")
                        .appendQueryParameter("usuario_id", String.valueOf(usuarioId));

                URL url = new URL(uriBuilder.build().toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                handleResponse(connection, response -> {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.getBoolean("success")) {
                        List<ImagenCapsulaRelation> relaciones = parseRelaciones(jsonResponse.getJSONArray("capsulas"));
                        callback.onSuccess(relaciones);
                    } else {
                        callback.onError(jsonResponse.getString("message"));
                    }
                }, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error obtener cápsulas: " + e.getMessage());
                callback.onError("Error de conexión");
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    // Eliminar cápsula
    public static void eliminarCapsula(int capsulaId, CapsulasCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                Uri.Builder params = new Uri.Builder()
                        .appendQueryParameter("action", "eliminar_capsula")
                        .appendQueryParameter("capsula_id", String.valueOf(capsulaId));

                URL url = new URL(BASE_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(params.build().getEncodedQuery().getBytes("UTF-8"));
                }

                handleResponse(connection, response -> {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.getBoolean("success")) {
                        callback.onSuccess(new ArrayList<>());
                    } else {
                        callback.onError(jsonResponse.getString("message"));
                    }
                }, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error eliminar cápsula: " + e.getMessage());
                callback.onError("Error de conexión");
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    // Editar cápsula
    public static void editarCapsula(Capsula capsula, CapsulasCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                Uri.Builder params = new Uri.Builder()
                        .appendQueryParameter("action", "editar_capsula")
                        .appendQueryParameter("capsula_id", String.valueOf(capsula.getId()))
                        .appendQueryParameter("titulo", capsula.getTitulo())
                        .appendQueryParameter("descripcion", capsula.getDescripcion());

                URL url = new URL(BASE_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(params.build().getEncodedQuery().getBytes("UTF-8"));
                }

                handleResponse(connection, response -> {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.getBoolean("success")) {
                        callback.onSuccess(new ArrayList<>());
                    } else {
                        callback.onError(jsonResponse.getString("message"));
                    }
                }, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error editar cápsula: " + e.getMessage());
                callback.onError("Error de conexión");
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    // Métodos auxiliares
    private static void writeMultipartField(DataOutputStream outputStream, String boundary,
                                            String fieldName, String value) throws Exception {
        outputStream.writeBytes("--" + boundary + "\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n\r\n");
        outputStream.writeBytes(value + "\r\n");
    }

    private static void handleResponse(HttpURLConnection connection, ResponseHandler handler, CapsulasCallback callback) {
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                handler.handleResponse(response.toString());
            } else {
                callback.onError("Error del servidor: " + responseCode);
            }
        } catch (Exception e) {
            callback.onError("Error procesando respuesta: " + e.getMessage());
        }
    }

    private static List<ImagenCapsulaRelation> parseRelaciones(JSONArray jsonArray) throws Exception {
        List<ImagenCapsulaRelation> relaciones = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonCapsula = jsonArray.getJSONObject(i);

            // Crear cápsula
            Capsula capsula = new Capsula(
                    jsonCapsula.getString("titulo"),
                    jsonCapsula.getString("descripcion"),
                    jsonCapsula.getDouble("latitud"),
                    jsonCapsula.getDouble("longitud")
            );
            capsula.setId(jsonCapsula.getInt("id"));

            // Crear imágenes
            List<Imagen> imagenes = new ArrayList<>();
            JSONArray jsonImagenes = jsonCapsula.getJSONArray("imagenes");
            for (int j = 0; j < jsonImagenes.length(); j++) {
                String url = jsonImagenes.getString(j);
            }

            // Crear relación
            ImagenCapsulaRelation relacion = new ImagenCapsulaRelation();
            relacion.capsula = capsula;
            relacion.imagenes = imagenes;

            relaciones.add(relacion);
        }
        return relaciones;
    }

    interface ResponseHandler {
        void handleResponse(String response) throws Exception;
    }
}