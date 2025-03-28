package com.example.das.ui.home;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.das.LocationService;
import com.example.das.R;
import com.example.das.activities.DetailCapsuleActivity;
import com.example.das.adapter.CapsulaAdapter;
import com.example.das.adapter.ImagenAdapter;
import com.example.das.data.database.AppDatabase;
import com.example.das.data.entity.Capsula;
import com.example.das.data.entity.Imagen;
import com.example.das.data.entity.ImagenCapsulaRelation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment implements CapsulaAdapter.OnCapsulaClickListener {

    private static final int PICK_IMAGES_REQUEST = 101;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1002;
    private static final int REQUEST_CODE_EDITAR_CAPSULA = 2;

    private AppDatabase db;
    private List<ImagenCapsulaRelation> listaCapsulas;
    private CapsulaAdapter adapter;
    private RecyclerView recyclerView;
    private ImagenAdapter imagenAdapter;
    private FusedLocationProviderClient fusedLocationClient;
    private Double currentLat = null;
    private Double currentLon = null;
    private MapView mapView;
    private AlertDialog currentDialog;

    // Variables para guardar estado del diálogo
    private boolean isDialogShowing = false;
    private String savedTitulo;
    private String savedDescripcion;
    private ArrayList<Uri> savedImagenes = new ArrayList<>();
    private SharedPreferences prefs;


    /**
     * Se encarga de inicializar servicios, restaurar estados previos y solicitar permisos.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent serviceIntent = new Intent(requireContext(), LocationService.class);
        ContextCompat.startForegroundService(requireContext(), serviceIntent);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        solicitarPermisosIniciales();

        // Restaurar estado guardado
        if (savedInstanceState != null) {
            isDialogShowing = savedInstanceState.getBoolean("dialog_showing", false);
            savedTitulo = savedInstanceState.getString("titulo");
            savedDescripcion = savedInstanceState.getString("descripcion");
            currentLat = savedInstanceState.getDouble("lat");
            currentLon = savedInstanceState.getDouble("lon");
            ArrayList<String> imagenesStrings = savedInstanceState.getStringArrayList("imagenes");
            savedImagenes.clear();
            if (imagenesStrings != null) {
                for (String uriString : imagenesStrings) {
                    savedImagenes.add(Uri.parse(uriString));
                }
            }
        }

    }

    /**
     * Se encarga de inicializar la base de datos, configurar la lista de cápsulas
     * y cargar datos de ejemplo si es la primera ejecución.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCapsulas);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        db = Room.databaseBuilder(requireContext(),
                        AppDatabase.class, "geocapsula_db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();


        // Inicializar SharedPreferences
        prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        // Verificar si es primera ejecución
        if (prefs.getBoolean("first_run", true)) {
            insertarDatosEjemplo();
            actualizarListaCapsulas();

            prefs.edit().putBoolean("first_run", false).apply();
        }
        actualizarListaCapsulas();

        adapter = new CapsulaAdapter(listaCapsulas, this);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(requireActivity().findViewById(R.id.main_toolbar));

        if (getActivity() != null) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
            }
        }

        FloatingActionButton fab = view.findViewById(R.id.fab);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewCapsulas);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && fab.isShown()) {
                    fab.hide();
                } else if (dy < 0 && !fab.isShown()) {
                    fab.show();
                }
            }
        });
        fab.setOnClickListener(v -> mostrarDialogoAgregarCapsula());

        // Restaurar diálogo si estaba abierto
        if (isDialogShowing) {
            mostrarDialogoAgregarCapsula(savedTitulo, savedDescripcion, savedImagenes);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentDialog != null && currentDialog.isShowing()) {
            outState.putBoolean("dialog_showing", true);

            EditText etTitulo = currentDialog.findViewById(R.id.etTitulo);
            if (etTitulo != null) {
                outState.putString("titulo", etTitulo.getText().toString());
            }
            EditText etDescripcion = currentDialog.findViewById(R.id.etDescripcion);
            if (etDescripcion != null) {
                outState.putString("descripcion", etDescripcion.getText().toString());
            }
            if (imagenAdapter != null) {
                List<Uri> imagenes = imagenAdapter.getImagenes();
                ArrayList<String> uris = new ArrayList<>();
                for (Uri uri : imagenes) {
                    uris.add(uri.toString());
                }
                outState.putStringArrayList("imagenes", uris);
            }
            outState.putDouble("lat", currentLat);
            outState.putDouble("lon", currentLon);
        } else {
            outState.putBoolean("dialog_showing", false);
        }
    }

    /**
     * Se ejecuta en un hilo secundario para evitar bloqueos en la UI.
     */
    private void insertarDatosEjemplo() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Capsula> capsulas = Arrays.asList(
                        new Capsula("Monumento Histórico", "Descripción del lugar emblemático", 40.416775, -3.703790),
                        new Capsula("Parque Natural", "Área verde con senderos para caminar", 41.385063, 2.173404),
                        new Capsula("Mirador Panorámico", "Vistas espectaculares de la ciudad", 40.712776, -74.005974)
                );

                int[] imagenes = {
                        R.drawable.imagen1,
                        R.drawable.imagen2,
                        R.drawable.imagen3
                };

                // Insertar cápsulas con imágenes específicas
                for (int i = 0; i < capsulas.size(); i++) {
                    Capsula capsula = capsulas.get(i);
                    long id = db.capsulaDao().insertarCapsula(capsula);

                    List<Integer> imagenesCapsula = new ArrayList<>();
                    switch (i) {
                        case 0:
                            imagenesCapsula.add(imagenes[0]);
                            imagenesCapsula.add(imagenes[1]);
                            break;
                        case 1:
                            imagenesCapsula.add(imagenes[2]);
                            imagenesCapsula.add(imagenes[1]);
                            break;
                        case 2:
                            imagenesCapsula.add(imagenes[1]);
                            imagenesCapsula.add(imagenes[0]);
                            imagenesCapsula.add(imagenes[2]);
                            break;
                    }

                    // Insertar imágenes
                    String packageName = requireContext().getPackageName();
                    for (int imagenRes : imagenesCapsula) {
                        String imageUri = "android.resource://" + packageName + "/" + imagenRes;
                        db.capsulaDao().insertarImagen(new Imagen((int) id, imageUri));
                    }
                }

                requireActivity().runOnUiThread(() -> {
                    actualizarListaCapsulas();
                    Toast.makeText(requireContext(), "Datos de ejemplo cargados", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e("HomeFragment", "Error insertando datos: " + e.getMessage());
            }
        });
    }

    /**
     * Aqui se muestra el dialog para agregar capsula si se ha movido la pantalla es decir se ha tumbado
     * se siguen mostrando los datos que el usuario habia puesto y se cambia de layout
     */
    private void mostrarDialogoAgregarCapsula() {
        mostrarDialogoAgregarCapsula(savedTitulo, savedDescripcion, savedImagenes);
    }

    /**
     * Aqui se muestra el dialog para agregar capsula
     */
    private void mostrarDialogoAgregarCapsula(String titulo, String descripcion, List<Uri> imagenes) {
        obtenerUbicacionActual();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_agregar_capsula, null);
        builder.setView(dialogView);

        // Configurar mapa
        mapView = dialogView.findViewById(R.id.mapView);
        inicializarMapa();

        // Configurar imágenes
        RecyclerView rvImagenes = dialogView.findViewById(R.id.rvImagenes);
        rvImagenes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        imagenAdapter = new ImagenAdapter();
        if (imagenes != null && !imagenes.isEmpty()) {
            imagenAdapter.agregarImagenes(imagenes);
        }
        rvImagenes.setAdapter(imagenAdapter);

        ImageView ivAddPhoto = dialogView.findViewById(R.id.ivAddPhoto);
        ivAddPhoto.setOnClickListener(v -> verificarPermisosArchivos());

        EditText etTitulo = dialogView.findViewById(R.id.etTitulo);
        EditText etDescripcion = dialogView.findViewById(R.id.etDescripcion);

        if (titulo != null) {
            etTitulo.setText(titulo);
        }
        if (descripcion != null) {
            etDescripcion.setText(descripcion);
        }

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            if (validarYCrearCapsula(etTitulo, etDescripcion)) {
                currentDialog.dismiss();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> currentDialog.dismiss());

        currentDialog = builder.create();
        currentDialog.show();

        // Manejar ciclo de vida del MapView
        mapView.onCreate(currentDialog.onSaveInstanceState());
        mapView.onResume();

        currentDialog.setOnDismissListener(dialog -> {
            isDialogShowing = false;
            savedTitulo = null;
            savedDescripcion = null;
            savedImagenes.clear();
        });

        isDialogShowing = true;
    }
    /**
     * Inicializa el mapa que se muestra en el dialog
     */
    private void inicializarMapa() {
        mapView.getMapAsync(googleMap -> {
            if (currentLat != null && currentLon != null) {
                LatLng ubicacion = new LatLng(currentLat, currentLon);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 15));
                googleMap.addMarker(new MarkerOptions().position(ubicacion));
            }
        });
    }

    private boolean validarYCrearCapsula(EditText etTitulo, EditText etDescripcion) {
        if (currentLat == null || currentLon == null) {
            Toast.makeText(requireContext(), "Error obteniendo ubicación", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!validarCampos(etTitulo, etDescripcion)) return false;

        guardarCapsulaConImagenes(
                etTitulo.getText().toString(),
                etDescripcion.getText().toString(),
                currentLat,
                currentLon,
                imagenAdapter.getImagenes()
        );
        return true;
    }

    private void verificarPermisosArchivos() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST_CODE);
        } else {
            abrirSelectorImagenes();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionActual();
            }
        }
        else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirSelectorImagenes();
            }
        }
    }

    /**
     * Obtener la ubiacion actual para cuando creamos una capsula
     */
    private void obtenerUbicacionActual() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            currentLat = location.getLatitude();
                            currentLon = location.getLongitude();
                            if (mapView != null) inicializarMapa();
                        }
                    });
        }
    }


    private boolean validarCampos(EditText... campos) {
        for (EditText campo : campos) {
            if (campo.getText().toString().trim().isEmpty()) {
                Toast.makeText(requireContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    /**
     * Solicitar permisos iniciales al usuario.
     */
    private void solicitarPermisosIniciales() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1003);
            }
        }
    }

    /**
     *
     * Guarda las imágenes seleccionadas en una lista para ser añadidas a la cápsula.
     */
    private void abrirSelectorImagenes() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_IMAGES_REQUEST);
    }

    /**
     * Cuando se edita una capsula se actualiza la lista para que se muestren los cambios.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Manejar selección de imágenes
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            List<Uri> uris = new ArrayList<>();

            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    tomarPermisoUri(uri);
                    uris.add(uri);
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                tomarPermisoUri(uri);
                uris.add(uri);
            }

            imagenAdapter.agregarImagenes(uris);
        }
        // Manejar actualización de cápsula
        else if (requestCode == REQUEST_CODE_EDITAR_CAPSULA && resultCode == Activity.RESULT_OK && data != null) {
            actualizarListaCapsulas();
        }
    }

    private void tomarPermisoUri(Uri uri) {
        requireActivity().getContentResolver().takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );
    }
    /**
     * Guardar una nueva cápsula en la base de datos.
     * Se ejecuta en un hilo secundario para evitar bloqueos en la interfaz de usuario.
     */
    private void guardarCapsulaConImagenes(String titulo, String descripcion, double latitud,
                                           double longitud, List<Uri> imagenes) {
        try {

            // 1. Guardar cápsula
            Capsula nuevaCapsula = new Capsula(titulo, descripcion, latitud, longitud);
            long capsulaId = db.capsulaDao().insertarCapsula(nuevaCapsula);

            // 2. Guardar imágenes relacionadas
            for (Uri uri : imagenes) {
                Imagen imagen = new Imagen((int) capsulaId, uri.toString());
                db.capsulaDao().insertarImagen(imagen);
            }

            actualizarListaCapsulas();
            mostrarNotificacionConfirmacion(titulo);
            Toast.makeText(requireContext(), "Cápsula guardada exitosamente", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Error en formato numérico", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Pushea la notificacion de capsula agregada
     */
    private void mostrarNotificacionConfirmacion(String tituloCapsula) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "CANAL_ID")
                    .setSmallIcon(R.drawable.notifications_24px)
                    .setContentTitle("Cápsula guardada")
                    .setContentText("'" + tituloCapsula + "' se ha guardado exitosamente")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());

            // Verificar que el canal de notificación existe
            NotificationChannel channel = notificationManager.getNotificationChannel("CANAL_ID");
            if (channel == null) {
                Toast.makeText(requireContext(), "Error: Canal de notificaciones no creado", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verificar y solicitar permiso si es necesario
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);

                    return;
                }
            }

            notificationManager.notify((int) System.currentTimeMillis(), builder.build());

        } catch (Exception e) {
            Log.e("Notificaciones", "Error al mostrar notificación: " + e.getMessage());
        }
    }

    /**
     * Actualizar la lista de cápsulas desde la base de datos
     * y notificar al adaptador de los cambios.
     */
    private void actualizarListaCapsulas() {
        listaCapsulas = db.capsulaDao().obtenerTodasCapsulasConImagenes();
        if (adapter != null) {
            adapter.actualizarDatos(listaCapsulas);
        }
    }

    /**
     *Para cuando hacemos click en una cápsula
     */
    @Override
    public void onCapsulaClick(List<Imagen> imagenes, Capsula capsula) {
        Intent intent = new Intent(getActivity(), DetailCapsuleActivity.class);
        intent.putExtra("imagenes", new ArrayList<>(imagenes));
        intent.putExtra("capsula", capsula);
        startActivityForResult(intent, REQUEST_CODE_EDITAR_CAPSULA);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.onDestroy();
        }
        if (db != null) {
            db.close();
        }
    }
}