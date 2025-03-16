package com.example.das.ui.profile;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.das.R;
import com.example.das.databinding.FragmentProfileBinding;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.Locale;

import androidx.appcompat.app.AppCompatDelegate;


public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        setupToolbarAndDrawer(root);
        return root;
    }


    //Preparamos el toolbar y el drawer
    private void setupToolbarAndDrawer(View root) {
        MaterialToolbar fragmentToolbar = root.findViewById(R.id.toolbar);

        // Configurar Toolbar del Fragment
        ((AppCompatActivity) requireActivity()).setSupportActionBar(fragmentToolbar);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false); // Opcional
        }

        // Configurar Navigation Drawer
        drawerLayout = root.findViewById(R.id.drawer_layout);
        NavigationView navigationView = root.findViewById(R.id.nav_drawer);

        drawerToggle = new ActionBarDrawerToggle(
                requireActivity(),
                drawerLayout,
                binding.toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            handleNavigationItemSelected(item);
            return true;
        });
    }

    //En el menu de navegacion cambiamos el idioma o el tema
    private void handleNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_language) {
            showLanguageDialog();
        } else if (id == R.id.nav_theme) {
            toggleTheme();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
    }

    /**
     * Dialog de eleccion de idioma
     */
    private void showLanguageDialog() {
        String[] languages = {"English", "Español"};
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_language)
                .setItems(languages, (dialog, which) -> {
                    String langCode = (which == 0) ? "en" : "es";
                    setAppLanguage(langCode);
                })
                .show();
    }

    /**
     * Guarda el idioma seleccionado en las preferencias y actualiza la configuración del idioma en la app.
     * Luego, reinicia la actividad para aplicar los cambios.
     */
    private void setAppLanguage(String langCode) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        prefs.edit().putString("language", langCode).apply();
        updateLanguageConfiguration(langCode);
        requireActivity().recreate();
    }

    /**
     * Aplica la configuración del idioma en la app utilizando el código de idioma proporcionado.
     */
    private void updateLanguageConfiguration(String langCode) {
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(new Locale(langCode));
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    /**
     * Alterna entre el modo claro y oscuro, guardando la preferencia en las configuraciones de la app.
     */
    private void toggleTheme() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        boolean isDarkTheme = prefs.getBoolean("dark_theme", false);
        prefs.edit().putBoolean("dark_theme", !isDarkTheme).apply();
        applyThemeConfiguration(!isDarkTheme);
    }

    /**
     * Aplica la configuración de tema (claro u oscuro) y reinicia la actividad si es necesario.
     */
    private void applyThemeConfiguration(boolean isDarkTheme) {
        Activity activity = requireActivity();
        if (activity instanceof AppCompatActivity) {
            AppCompatDelegate delegate = ((AppCompatActivity) activity).getDelegate();
            int nightMode = isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

            if (delegate.getLocalNightMode() != nightMode) {
                delegate.setLocalNightMode(nightMode);
                activity.recreate();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (drawerLayout != null) {
            drawerLayout.removeDrawerListener(drawerToggle);
        }
    }
}