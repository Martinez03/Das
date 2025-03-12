package com.example.das.ui.home;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

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
import java.util.List;

public class HomeFragment extends Fragment implements CapsulaAdapter.OnCapsulaClickListener {

    private static final int PICK_IMAGES_REQUEST = 101;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1002;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        solicitarPermisosIniciales();
    }

    private void solicitarPermisosIniciales() {
        // Solicitar permisos de ubicación al iniciar
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

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

        actualizarListaCapsulas();
        adapter = new CapsulaAdapter(listaCapsulas, this);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton fab = view.findViewById(R.id.fab);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewCapsulas);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && fab.isShown()) {
                    fab.hide(); // Oculta el FAB al hacer scroll hacia abajo
                } else if (dy < 0 && !fab.isShown()) {
                    fab.show(); // Muestra el FAB al hacer scroll hacia arriba
                }
            }
        });
        fab.setOnClickListener(v -> mostrarDialogoAgregarCapsula());
    }

    private void mostrarDialogoAgregarCapsula() {
        obtenerUbicacionActual();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_capsula, null);
        builder.setView(dialogView);

        // Configurar mapa
        mapView = dialogView.findViewById(R.id.mapView);
        inicializarMapa();

        // Configurar imágenes
        RecyclerView rvImagenes = dialogView.findViewById(R.id.rvImagenes);
        rvImagenes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        imagenAdapter = new ImagenAdapter();
        rvImagenes.setAdapter(imagenAdapter);

        ImageView ivAddPhoto = dialogView.findViewById(R.id.ivAddPhoto); // Asegúrate de agregar este ID en el XML
        ivAddPhoto.setOnClickListener(v -> verificarPermisosArchivos());

        EditText etTitulo = dialogView.findViewById(R.id.etTitulo);
        EditText etDescripcion = dialogView.findViewById(R.id.etDescripcion);

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
    }

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

    private void abrirSelectorImagenes() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_IMAGES_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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
    }

    private void tomarPermisoUri(Uri uri) {
        requireActivity().getContentResolver().takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );
    }

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
            Toast.makeText(requireContext(), "Cápsula guardada exitosamente", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Error en formato numérico", Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarListaCapsulas() {
        listaCapsulas = db.capsulaDao().obtenerTodasCapsulasConImagenes();
        if (adapter != null) {
            adapter.actualizarDatos(listaCapsulas);
        }
    }

    @Override
    public void onCapsulaClick(List<String> imagenes, double latitud, double longitud) {
        Intent intent = new Intent(getActivity(), DetailCapsuleActivity.class);

        // Las imágenes ya vienen como List<String> desde el adaptador
        intent.putStringArrayListExtra("imagenes", new ArrayList<>(imagenes));
        intent.putExtra("lat", latitud);  // Usar los parámetros del método
        intent.putExtra("lng", longitud); // en lugar de currentLat/currentLon

        startActivity(intent);
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