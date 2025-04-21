// LocationService.java
package com.example.das;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.das.activities.DetailCapsuleActivity;
import com.example.das.data.entity.Capsula;
import com.example.das.data.entity.ImagenCapsulaRelation;
import com.example.das.webservice.CapsulasWebService;
import com.example.das.widget.CapsulasWidgetProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocationService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private static final float RADIO_MARGEN_KM = 0.1f; // 100 metros
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        iniciarServicioForeground();
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        startLocationUpdates();
    }

    //Permisos de ubicacion

    private void startLocationUpdates() {
        LocationRequest peticion = new LocationRequest.Builder(10000)
                .setMinUpdateIntervalMillis(5000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(peticion, locationCallback, null);
        } else {
            Log.e("LocationService", "Falta el permiso ACCESS_FINE_LOCATION");
        }
    }


    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult != null && locationResult.getLastLocation() != null) {
                Location location = locationResult.getLastLocation();
                obtenerYVerificarCapsulas(location.getLatitude(), location.getLongitude());
            }
        }
    };

    //Iniciamos servicio en segundo plano
    private void iniciarServicioForeground() {
        Notification notification = new NotificationCompat.Builder(this, "CANAL_CAPSULAS")
                .setContentTitle("Monitoreando ubicación")
                .setContentText("Buscando cápsulas cercanas...")
                .setSmallIcon(R.drawable.pin_drop_24px)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    //Verificamos las capsulas cercanas
    private void obtenerYVerificarCapsulas(double currentLat, double currentLon) {
        int usuarioId = prefs.getInt("usuario_id", -1);
        CapsulasWebService.obtenerCapsulasPorUsuario(usuarioId, new CapsulasWebService.CapsulasCallback() {
            @Override
            public void onSuccessLista(List<ImagenCapsulaRelation> relaciones) {
                verificarCapsulasCercanas(currentLat, currentLon, relaciones);
            }

            @Override
            public void onSuccess(int capsulaId) {}

            @Override
            public void onError(String message) {
                Log.e("LocationService", "Error al obtener cápsulas: " + message);
            }
        });
    }

    private void verificarCapsulasCercanas(double currentLat, double currentLon, List<ImagenCapsulaRelation> relaciones) {
        SharedPreferences sp = getSharedPreferences("notified_capsulas", MODE_PRIVATE);
        Set<String> notificados = sp.getStringSet("capsulas_notificadas", new HashSet<>());

        for (ImagenCapsulaRelation relacion : relaciones) {
            Capsula capsula = relacion.capsula;
            double distancia = calcularDistancia(currentLat, currentLon, capsula.getLatitud(), capsula.getLongitud());

            if (distancia <= RADIO_MARGEN_KM && !notificados.contains(String.valueOf(capsula.getId()))) {
                mostrarNotificacionProximidad(capsula);
                notificados.add(String.valueOf(capsula.getId()));
            }
        }

        sp.edit().putStringSet("capsulas_notificadas", notificados).apply();
    }


    //Calculamos distancias de la capsula a nuestra ubi para que esten en un radio de 100 metros
    public static double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Radio de la Tierra en km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distancia en km
    }


    //Pusheamos notifiacion
    private void mostrarNotificacionProximidad(Capsula capsula) {
        try {
            // Intent para abrir la cápsula al hacer clic en la notificación
            Intent intent = new Intent(this, DetailCapsuleActivity.class);
            intent.putExtra("capsula", capsula);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
            );
            // Construir la notificación
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CANAL_CAPSULAS")
                    .setSmallIcon(R.drawable.add_24px)
                    .setContentTitle("¡Cápsula cercana!")
                    .setContentText("Estás cerca de: " + capsula.getTitulo())
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent) // Acción al hacer clic
                    .setAutoCancel(true); // Cierra la notificación al hacer clic

            // Mostrar la notificación
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(capsula.getId(), builder.build());
            } else {
                Log.e("Notificaciones", "Permiso de notificaciones denegado");
            }
            Intent intentWidget = new Intent(CapsulasWidgetProvider.ACTION_ACTUALIZAR_WIDGET);
            intentWidget.setPackage(getPackageName());
            sendBroadcast(intentWidget);
        } catch (Exception e) {
            Log.e("Notificaciones", "Error al mostrar notificación: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
