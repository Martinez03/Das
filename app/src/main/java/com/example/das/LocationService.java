// LocationService.java
package com.example.das;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.room.Room;

import com.example.das.activities.DetailCapsuleActivity;
import com.example.das.data.database.AppDatabase;
import com.example.das.data.entity.Capsula;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocationService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private AppDatabase db; // Instancia de la base de datos
    private static final int UPDATE_INTERVAL = 5000; // 5 segundos
    private static final float RADIO_MARGEN_KM = 0.1f; // 100 metros

    @Override
    public void onCreate() {
        super.onCreate();

        db = Room.databaseBuilder(this,
                        AppDatabase.class, "geocapsula_db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        iniciarServicioForeground();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setInterval(UPDATE_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, null);
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
                verificarCapsulasCercanas(location.getLatitude(), location.getLongitude());
            }
        }
    };
    private void iniciarServicioForeground() {
        Notification notification = new NotificationCompat.Builder(this, "CANAL_CAPSULAS")
                .setContentTitle("Monitoreando ubicación")
                .setContentText("Buscando cápsulas cercanas...")
                .setSmallIcon(R.drawable.add_24px)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }
    private void verificarCapsulasCercanas(double currentLat, double currentLon) {
        SharedPreferences sp = getSharedPreferences("notified_capsulas", MODE_PRIVATE);
        Set<String> notificados = sp.getStringSet("capsulas_notificadas", new HashSet<>());
        List<Capsula> todasLasCapsulas = db.capsulaDao().obtenerCapsulas();
        for (Capsula capsula : todasLasCapsulas) {
            double distancia = calcularDistancia(currentLat, currentLon, capsula.getLatitud(), capsula.getLongitud());
            if (distancia <= RADIO_MARGEN_KM && !notificados.contains(String.valueOf(capsula.getId()))) {
                mostrarNotificacionProximidad(capsula);
                notificados.add(String.valueOf(capsula.getId()));
            }
        }
        sp.edit().putStringSet("capsulas_notificadas", notificados).apply();
    }


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
                    .setAutoCancel(true) // Cierra la notificación al hacer clic
                    .setVibrate(new long[]{0, 500, 250, 500}) // Vibración
                    .setDefaults(Notification.DEFAULT_SOUND); // Sonido por defecto

            // Mostrar la notificación
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(capsula.getId(), builder.build());
            } else {
                Log.e("Notificaciones", "Permiso de notificaciones denegado");
            }
        } catch (Exception e) {
            Log.e("Notificaciones", "Error al mostrar notificación: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Este servicio no es ligado
    }
}
