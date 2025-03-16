package com.example.das;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.das.databinding.ActivityMainBinding;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;


import java.util.Locale;

// Para ProfileFragment
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MaterialToolbar mainToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeSettings();
        applyLanguageConfiguration();
        super.onCreate(savedInstanceState);
        crearCanalNotificaciones();
        crearCanalNotificacionesCapsulaCerca();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mainToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);

        setupNavigation();

    }
    private void crearCanalNotificaciones() {
        NotificationChannel channel = new NotificationChannel(
                "CANAL_ID",
                "Cápsulas",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notificaciones de cápsulas guardadas");
        channel.enableLights(true);
        channel.setLightColor(Color.BLUE);


        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }
    private void crearCanalNotificacionesCapsulaCerca() {
        NotificationChannel channel = new NotificationChannel(
                "CANAL_CAPSULAS",
                "Notificaciones de cápsulas",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notificaciones cuando estás cerca de una cápsula");
        channel.enableLights(true);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }


    private void setupNavigation() {
        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home,
                R.id.navigation_map,
                R.id.navigation_profile
        ).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        // Listener para controlar la visibilidad del Toolbar
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.navigation_profile) {
                mainToolbar.setVisibility(View.GONE);
            } else {
                mainToolbar.setVisibility(View.VISIBLE);
            }
        });
    }
    private void applyThemeSettings() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean isDarkTheme = prefs.getBoolean("dark_theme", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void applyLanguageConfiguration() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String lang = prefs.getString("language", "en");
        setLocale(this, lang);
    }

    public static void setLocale(Context context, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}