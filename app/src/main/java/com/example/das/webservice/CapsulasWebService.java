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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CapsulasWebService {

    private static final String BASE_URL = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/pmartinez084/WEB/capsulas.php";
    private static final String TAG = "CapsulasWebService";

    public interface CapsulasCallback {
        void onSuccessLista(List<ImagenCapsulaRelation> capsulasConImagenes);
        void onSuccess(int capsulaId);
        void onError(String mensaje);
    }

    // Crear cápsula con imágenes
    public static void crearCapsula(Context context, int usuarioId,
                                    List<Imagen> imagenes, String titulo, String descripcion,
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
                    if (imagenes != null && !imagenes.isEmpty()) {
                        for (Imagen imagen : imagenes) {
                            try {
                                // Convertir directamente desde el byte[]
                                String fotoBase64 = Base64.encodeToString(imagen.getFoto(), Base64.NO_WRAP);

                                // Optimizar compresión
                                Bitmap bitmap = BitmapFactory.decodeByteArray(imagen.getFoto(), 0, imagen.getFoto().length);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.WEBP, 85, stream); // Mejor formato y compresión
                                fotoBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);

                                writeMultipartField(dataOutputStream, boundary, "imagen[]", fotoBase64);
                            } catch (Exception e) {
                                Log.e(TAG, "Error procesando imagen: " + e.getMessage());
                            }
                        }
                    }

                    dataOutputStream.writeBytes("--" + boundary + "--\r\n");
                }
                handleResponse(connection, response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            int capsulaId = jsonResponse.getInt("capsula_id");
                            callback.onSuccess(capsulaId); // Llamar al callback correcto
                        } else {
                            callback.onError(jsonResponse.getString("message"));
                        }
                    } catch (JSONException e) {
                        callback.onError("Error en formato de respuesta");
                    }
                }, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error crear cápsula: " + e.getMessage());
                callback.onError("Error de conexión");
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

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
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray capsulasArray = jsonResponse.getJSONArray("capsulas");
                            List<ImagenCapsulaRelation> relaciones = new ArrayList<>();

                            for (int i = 0; i < capsulasArray.length(); i++) {
                                JSONObject capsulaObj = capsulasArray.getJSONObject(i);
                                int id = capsulaObj.getInt("id");
                                String titulo = capsulaObj.getString("titulo");
                                String descripcion = capsulaObj.getString("descripcion");
                                double latitud = capsulaObj.getDouble("latitud");
                                double longitud = capsulaObj.getDouble("longitud");

                                // Procesar imágenes
                                // Crear lista para las imágenes de esta cápsula
                                List<Imagen> imagenes = new ArrayList<>();

                                // Obtener las imágenes de la cápsula
                                JSONArray imagenesArray = capsulaObj.getJSONArray("imagenes");
                                for (int j = 0; j < imagenesArray.length(); j++) {
                                    String base64 = imagenesArray.getJSONObject(j).getString("imagen");
                                    byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);

                                    // Crear objeto Imagen con byte[] (ya no necesitamos Bitmap)
                                    Imagen imagen = new Imagen(id, decodedBytes);

                                    // Agregar la imagen a la lista
                                    if (decodedBytes != null) {
                                        imagenes.add(imagen);
                                    }
                                }

                                Capsula capsula = new Capsula(titulo,descripcion,latitud,longitud);
                                capsula.setId(id);
                                ImagenCapsulaRelation relacion = new ImagenCapsulaRelation();
                                relacion.capsula = capsula;
                                relacion.imagenes = imagenes;
                                relaciones.add(relacion);
                            }

                            callback.onSuccessLista(relaciones);
                        } else {
                            callback.onError(jsonResponse.getString("message"));
                        }
                    } catch (Exception e) {
                        callback.onError("Error al procesar la respuesta");
                        e.printStackTrace();
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
                // URL con el parámetro de acción
                Uri.Builder uriBuilder = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter("action", "eliminar_capsula");

                URL url = new URL(uriBuilder.build().toString());
                connection = (HttpURLConnection) url.openConnection();
                String parametros = "capsula_id=" + URLEncoder.encode(String.valueOf(capsulaId), "UTF-8");

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                PrintWriter out = new PrintWriter(connection.getOutputStream());
                out.print(parametros);
                out.close();
                 handleResponse(connection, response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            callback.onSuccess(capsulaId);
                        } else {
                            callback.onError(jsonResponse.getString("message"));
                        }
                    } catch (Exception e) {
                        callback.onError("Error al procesar la respuesta");
                    }
                }, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error al eliminar cápsula: " + e.getMessage());
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
                Uri.Builder uriBuilder = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter("action", "editar_capsula");
                URL url = new URL(uriBuilder.build().toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // Construimos los parámetros manualmente
                String parametros = "&capsula_id=" + URLEncoder.encode(String.valueOf(capsula.getId()), "UTF-8") +
                        "&titulo=" + URLEncoder.encode(capsula.getTitulo(), "UTF-8") +
                        "&descripcion=" + URLEncoder.encode(capsula.getDescripcion(), "UTF-8");

                // Enviamos con PrintWriter como pediste
                try (PrintWriter out = new PrintWriter(connection.getOutputStream())) {
                    out.print(parametros);
                    out.close();
                }

                handleResponse(connection, response -> {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.getBoolean("success")) {
                        callback.onSuccessLista(new ArrayList<>()); // Lista vacía por convención
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