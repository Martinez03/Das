package com.example.das.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import java.util.HashSet;
import com.example.das.R;

public class CapsulasWidgetProvider extends AppWidgetProvider {

    // Acción personalizada para actualización manual
    public static final String ACTION_ACTUALIZAR_WIDGET = "com.example.das.ACTUALIZAR_WIDGET_CAPSULAS";

    // Método estático para actualizar una instancia específica
    static void actualizarWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences("notified_capsulas", Context.MODE_PRIVATE);
        int totalCapsulas = prefs.getStringSet("capsulas_notificadas", new HashSet<>()).size();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_capsulas);
        views.setTextViewText(R.id.widget_text, "Cápsulas cercanas: " + totalCapsulas);

        // Configurar PendingIntent para actualizar al hacer clic
        Intent intent = new Intent(context, CapsulasWidgetProvider.class);
        intent.setAction(ACTION_ACTUALIZAR_WIDGET);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_text, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            actualizarWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_ACTUALIZAR_WIDGET.equals(intent.getAction())) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                actualizarWidget(context, appWidgetManager, appWidgetId);
            }
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            super.onReceive(context, intent);
        }
    }
}