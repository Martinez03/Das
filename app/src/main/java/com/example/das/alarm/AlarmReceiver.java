package com.example.das.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.das.MainActivity;
import android.Manifest;
import com.example.das.R;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        mostrarNotificacion(context);
    }

    private void mostrarNotificacion(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Crear canal de notificación (necesario para Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "canal_recordatorio",
                    "Recordatorios de cápsulas",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Intent para abrir la app al hacer clic en la notificación
        Intent intentApp = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intentApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Construir notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "canal_recordatorio")
                .setSmallIcon(R.drawable.notifications_24px)
                .setContentTitle("¡Hora de crear una cápsula!")
                .setContentText("No olvides registrar tu experiencia diaria")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Mostrar notificación
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build());
        }
    }
}