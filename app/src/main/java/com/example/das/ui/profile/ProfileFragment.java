package com.example.das.ui.profile;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.das.LocationService;
import com.example.das.R;
import com.example.das.alarm.AlarmReceiver;
import com.example.das.databinding.FragmentProfileBinding;
import com.example.das.webservice.UsuariosWebService;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import androidx.appcompat.app.AppCompatDelegate;
import com.example.das.alarm.AlarmReceiver;
import java.util.Calendar;
import android.app.AlarmManager;
import android.content.Intent;
import android.app.PendingIntent;


public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    private SharedPreferences prefs;

    private Button btnLogin, btnLogout;;
    private LinearLayout layoutProfile, layoutNoLogin;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        setupToolbarAndDrawer(root);
        btnLogin = root.findViewById(R.id.btnLogin);
        btnLogout = root.findViewById(R.id.btnLogout);
        layoutProfile = root.findViewById(R.id.layout_profile);
        layoutNoLogin = root.findViewById(R.id.layout_no_login);
        prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        btnLogin.setOnClickListener(v -> mostrarDialogoLogin());
        btnLogout.setOnClickListener(v -> cerrarSesion());
        Button btnAlarm = root.findViewById(R.id.btnAlarm);
        btnAlarm.setOnClickListener(v -> {
            Calendar calendario = Calendar.getInstance();
            int hora = calendario.get(Calendar.HOUR_OF_DAY);
            int minuto = calendario.get(Calendar.MINUTE);
            TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                    (view, selectedHour, selectedMinute) -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
                            if (alarmManager.canScheduleExactAlarms()) {
                                programarAlarmaDiaria(selectedHour, selectedMinute);
                            } else {
                                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                startActivity(intent);
                            }
                        } else {
                            programarAlarmaDiaria(selectedHour, selectedMinute);
                        }
                    }, hora, minuto, true);

            timePickerDialog.show();
        });
        checkLoginState();
        return root;
    }

    private void checkLoginState() {
        boolean isLoggedIn = prefs.contains("usuario_id");

        if (isLoggedIn) {
            // Mostrar perfil y ocultar botón de login
            layoutProfile.setVisibility(View.VISIBLE);
            layoutNoLogin.setVisibility(View.GONE);
            cargarDatosUsuario();
        } else {
            // Mostrar solo botón de login
            layoutProfile.setVisibility(View.GONE);
            layoutNoLogin.setVisibility(View.VISIBLE);
        }
    }

    private void cargarDatosUsuario() {
        View view = getView();
        if (view != null) {
            TextView tvNombre = view.findViewById(R.id.profile_name);
            TextView tvEmail = view.findViewById(R.id.profile_email);

            tvNombre.setText("SS");
            tvEmail.setText("SS");
        }
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

    private void mostrarDialogoLogin() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_login, null);


        EditText etCorreo = dialogView.findViewById(R.id.etCorreo);
        EditText etContrasena = dialogView.findViewById(R.id.etContrasena);
        Button btnRegistro = dialogView.findViewById(R.id.btnRegistro);

        builder.setView(dialogView)
                .setTitle("Iniciar Sesión")
                .setPositiveButton("Login", (dialog, which) -> {
                    String correo = etCorreo.getText().toString().trim();
                    String contrasena = etContrasena.getText().toString().trim();

                    if (!correo.isEmpty() && !contrasena.isEmpty()) {
                        UsuariosWebService.loginUsuario(
                                requireContext(),
                                correo,
                                contrasena,
                                () -> {
                                    if (isAdded()) {
                                        checkLoginState();
                                        Toast.makeText(requireContext(), "Login exitoso", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(requireContext(), "Ingrese todos los campos", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        android.app.AlertDialog dialog = builder.create();

        // Acción para el botón de registro
        btnRegistro.setOnClickListener(v -> {
            dialog.dismiss();
            mostrarDialogoRegistro();
        });

        dialog.show();
    }


    private void mostrarDialogoRegistro() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_register, null);

        EditText etNombre = dialogView.findViewById(R.id.etNombre);
        EditText etCorreo = dialogView.findViewById(R.id.etCorreo);
        EditText etContrasena = dialogView.findViewById(R.id.etContrasena);

        builder.setView(dialogView)
                .setTitle("Crear Cuenta")
                .setPositiveButton("Registrar", (dialog, which) -> {
                    String nombre = etNombre.getText().toString().trim();
                    String correo = etCorreo.getText().toString().trim();
                    String contrasena = etContrasena.getText().toString().trim();

                    if (!nombre.isEmpty() && !correo.isEmpty() && !contrasena.isEmpty()) {
                        registrarUsuario(nombre, correo, contrasena);
                    } else {
                        Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private static final int ALARM_REQUEST_CODE = 123;

    private void programarAlarmaDiaria(int hora, int minuto) {
        Context context = requireContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hora);
        calendar.set(Calendar.MINUTE, minuto);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(
                calendar.getTimeInMillis(),
                pendingIntent
        );

        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
    }


    private void registrarUsuario(String nombre, String correo, String contrasena) {
        UsuariosWebService.registrarUsuario(nombre, correo, contrasena, new UsuariosWebService.RegistroCallback() {
            @Override
            public void onExito(String mensaje) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
                    mostrarDialogoLogin();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void cerrarSesion() {
        prefs.edit().clear().apply();
        Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show();
        checkLoginState();
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