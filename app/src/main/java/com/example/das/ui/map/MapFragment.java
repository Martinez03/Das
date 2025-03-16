package com.example.das.ui.map;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.room.Room;

import com.example.das.adapter.CapsulaAdapter;
import com.example.das.data.database.AppDatabase;
import com.example.das.data.entity.Capsula;
import com.example.das.activities.DetailCapsuleActivity;
import com.example.das.data.entity.Imagen;
import com.example.das.data.entity.ImagenCapsulaRelation;
import com.example.das.databinding.FragmentMapBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback, CapsulaAdapter.OnCapsulaClickListener {

    private FragmentMapBinding binding;
    private MapView mapView;
    private GoogleMap googleMap;
    private AppDatabase db;
    private CapsulaAdapter adapter;
    private List<ImagenCapsulaRelation> listaCapsulas;
    private static final int REQUEST_CODE_EDITAR_CAPSULA = 2;

    /**
     * Se ejecuta al crear el fragmento. Inicializa la base de datos,
     * obtiene la lista de cápsulas y configura el adaptador.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = Room.databaseBuilder(requireContext(),
                        com.example.das.data.database.AppDatabase.class, "geocapsula_db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
        // Actualizamos la lista antes de crear el adapter
        actualizarListaCapsulas();
        adapter = new CapsulaAdapter(listaCapsulas, this);
    }

    /**
     * Obtiene la lista actualizada de cápsulas desde la base de datos
     * y notifica al adaptador para actualizar la vista.
     */
    private void actualizarListaCapsulas() {
        listaCapsulas = db.capsulaDao().obtenerTodasCapsulasConImagenes();
        if (adapter != null) {
            adapter.actualizarDatos(listaCapsulas);
        }
    }

    /**
     * Maneja el resultado de actividades. Si una cápsula ha sido editada,
     * se actualiza la lista de cápsulas en la vista.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDITAR_CAPSULA && resultCode == Activity.RESULT_OK && data != null) {
            actualizarListaCapsulas();
        }
    }

    /**
     * Infla el diseño del fragmento y configura el mapa.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mapView = binding.mapView;
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        return root;
    }

    /**
     * Se ejecuta cuando el mapa está listo. Carga las cápsulas y las
     * representa como marcadores en el mapa.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        listaCapsulas = db.capsulaDao().obtenerTodasCapsulasConImagenes();

        // Agregar un marcador por cada cápsula y asociar el objeto como tag
        for (ImagenCapsulaRelation relacion : listaCapsulas) {
            Capsula capsula = relacion.capsula;
            double lat = capsula.getLatitud();
            double lon = capsula.getLongitud();
            LatLng posicion = new LatLng(lat, lon);
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(posicion)
                    .title(capsula.getTitulo()));
            marker.setTag(capsula);
        }

        // Configurar listener para clicks en marcadores
        googleMap.setOnMarkerClickListener(marker -> {
            Capsula seleccionada = (Capsula) marker.getTag();
            List<Imagen> imagenes = db.capsulaDao().obtenerImagenesPorCapsula(seleccionada.getId());
            onCapsulaClick(imagenes, seleccionada);
            return false;
        });

        // Opcional: mover la cámara a la primera cápsula o a una posición central
        if (!listaCapsulas.isEmpty()) {
            ImagenCapsulaRelation primera = listaCapsulas.get(0);
            LatLng posicionInicial = new LatLng(primera.capsula.getLatitud(), primera.capsula.getLongitud());
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionInicial, 12));
        }
    }

    /**
     * Abre la actividad de detalles de una cápsula cuando se selecciona en el mapa.
     */
    @Override
    public void onCapsulaClick(List<Imagen> imagenes, Capsula capsula) {
        Intent intent = new Intent(getActivity(), DetailCapsuleActivity.class);
        intent.putExtra("imagenes", new ArrayList<>(imagenes));
        intent.putExtra("capsula", capsula);
        startActivityForResult(intent, REQUEST_CODE_EDITAR_CAPSULA);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.onDestroy();
        }
        binding = null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }
}
