package com.example.das.webservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UsuariosWebService {

    private SharedPreferences prefs;

    // Registrar usuario
    public static void registrarUsuario(String nombre, String correo, String contrasena, RegistroCallback callback) {
        new Thread(() -> {
            try {
                String direccion = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/pmartinez084/WEB/usuarios.php?action=register";

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("nombre", nombre)
                        .appendQueryParameter("correo", correo)
                        .appendQueryParameter("contrasena", contrasena);

                String parametros = builder.build().getEncodedQuery();

                URL url = new URL(direccion);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                OutputStream os = urlConnection.getOutputStream();
                os.write(parametros.getBytes("UTF-8"));
                os.close();

                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.getBoolean("success")) {
                        callback.onExito("Registro exitoso");
                    } else {
                        callback.onError(jsonResponse.getString("message"));
                    }
                } else {
                    callback.onError("Error del servidor: " + statusCode);
                }

                urlConnection.disconnect();
            } catch (Exception e) {
                callback.onError("Error de conexión: " + e.getMessage());
            }
        }).start();
    }

    public interface RegistroCallback {
        void onExito(String mensaje);
        void onError(String error);
    }

    // Login usuario
    public static void loginUsuario(Context context, String correo, String contrasena, Runnable onLoginSuccess) {
        new Thread(() -> {
            try {
                String direccion = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/pmartinez084/WEB/usuarios.php?action=login";

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("correo", correo)
                        .appendQueryParameter("contrasena", contrasena);

                String parametros = builder.build().getEncodedQuery();

                URL url = new URL(direccion);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                OutputStream os = urlConnection.getOutputStream();
                os.write(parametros.getBytes("UTF-8"));
                os.close();

                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    Log.d("UsuariosWebService", "Respuesta: " + response);

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.getBoolean("success")) {
                        JSONObject usuario = jsonResponse.getJSONObject("usuario");
                        int usuarioId = usuario.getInt("id");
                        String nombreUsuario = usuario.getString("nombre");

                        // Guardar en SharedPreferences
                        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putInt("usuario_id", usuarioId)
                                .putString("nombre_usuario", nombreUsuario)
                                .apply();

                        // Notificar al hilo principal
                        if (onLoginSuccess != null) {
                            new Handler(Looper.getMainLooper()).post(onLoginSuccess);
                        }
                    } else {
                        Log.e("UsuariosWebService", "Login fallido: " + jsonResponse.optString("message"));
                    }
                } else {
                    Log.e("UsuariosWebService", "Código HTTP: " + statusCode);
                }

                urlConnection.disconnect();
            } catch (Exception e) {
                Log.e("UsuariosWebService", "Error al hacer login", e);
            }
        }).start();
    }


}
