package com.example.das.ui.map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.room.Room;

import com.example.das.LocationService;
import com.example.das.R;
import com.example.das.adapter.CapsulaAdapter;
import com.example.das.data.database.AppDatabase;
import com.example.das.data.entity.Capsula;
import com.example.das.activities.DetailCapsuleActivity;
import com.example.das.data.entity.Imagen;
import com.example.das.data.entity.ImagenCapsulaRelation;
import com.example.das.databinding.FragmentMapBinding;
import com.example.das.webservice.CapsulasWebService;
import com.example.das.webservice.UsuariosWebService;
import com.google.android.gms.location.LocationServices;
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
    private CapsulaAdapter adapter;
    private List<ImagenCapsulaRelation> listaCapsulas;
    private static final int REQUEST_CODE_EDITAR_CAPSULA = 2;

    private Button btnLogin;
    private SharedPreferences prefs;

    /**
     * Se ejecuta al crear el fragmento. Inicializa la base de datos,
     * obtiene la lista de cápsulas y configura el adaptador.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        adapter = new CapsulaAdapter(listaCapsulas, this);
    }

    /**
     * Obtiene la lista actualizada de cápsulas desde la base de datos
     * y notifica al adaptador para actualizar la vista.
     */
    private void actualizarListaCapsulas() {
        // Llamamos al servicio para obtener las cápsulas
        int usuarioId = prefs.getInt("usuario_id", -1);
        CapsulasWebService.obtenerCapsulasPorUsuario(usuarioId,new CapsulasWebService.CapsulasCallback() {
            @Override
            public void onSuccessLista(List<ImagenCapsulaRelation> relaciones) {
                requireActivity().runOnUiThread(() -> {
                    listaCapsulas = relaciones;
                    if (adapter != null) {
                        adapter.actualizarDatos(listaCapsulas);
                    }
                });
            }
            @Override
            public void onSuccess(int capsulaId) {
                Log.d("Actualización", "Cápsula con ID: " + capsulaId + " procesada correctamente.");
            }


            @Override
            public void onError(String message) {
                // Manejo de errores
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error al obtener las cápsulas: " + message, Toast.LENGTH_SHORT).show()
                );

            }
        });
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
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        btnLogin = view.findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> mostrarDialogoLogin());
        mapView = binding.mapView;
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        checkLoginState();
        return root;
    }

    private void mostrarDialogoLogin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
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

        AlertDialog dialog = builder.create();

        // Acción para el botón de registro
        btnRegistro.setOnClickListener(v -> {
            dialog.dismiss();
            mostrarDialogoRegistro();
        });

        dialog.show();
    }


    private void mostrarDialogoRegistro() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
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

    private void registrarUsuario(String nombre, String correo, String contrasena) {
        UsuariosWebService.registrarUsuario(nombre, correo, contrasena, new UsuariosWebService.RegistroCallback() {
            @Override
            public void onExito(String mensaje) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
                    mostrarDialogoLogin(); // Vuelve al login después del registro
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show());
            }
        });
    }




    private void checkLoginState() {
        boolean isLoggedIn = prefs.contains("usuario_id");
        if (isLoggedIn) {
            mapView.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
            actualizarListaCapsulas();
        } else {
            mapView.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Se ejecuta cuando el mapa está listo. Carga las cápsulas y las
     * representa como marcadores en el mapa.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        actualizarListaCapsulas();
        // Comprobar si la lista de cápsulas no está vacía
        if (listaCapsulas != null && !listaCapsulas.isEmpty()) {
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

            // Opcional: mover la cámara a la primera cápsula o a una posición central
            ImagenCapsulaRelation primera = listaCapsulas.get(0);
            LatLng posicionInicial = new LatLng(primera.capsula.getLatitud(), primera.capsula.getLongitud());
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionInicial, 12));
        } else {
            // Manejo de caso cuando no hay cápsulas
            Log.w("MapFragment", "No hay cápsulas para mostrar en el mapa.");
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
