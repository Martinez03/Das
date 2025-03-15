package com.example.das.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.bumptech.glide.Glide;
import com.example.das.R;
import com.example.das.adapter.ImagenAdapter;
import com.example.das.data.database.AppDatabase;
import com.example.das.data.entity.Capsula;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class DetailCapsuleActivity extends AppCompatActivity implements OnMapReadyCallback {
    private AppDatabase db;
    private MapView mapView;
    private GoogleMap googleMap;
    private double latitude;
    private double longitude;
    private Capsula capsula;
    private String capsuleTitle;
    private String capsuleDescription;

    private androidx.appcompat.widget.AppCompatTextView tvCapsuleTitle;
    private androidx.appcompat.widget.AppCompatTextView tvCapsuleDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        db = Room.databaseBuilder(this, AppDatabase.class, "geocapsula_db")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_capsule);
        capsula = (Capsula) getIntent().getSerializableExtra("capsula");

        // Configurar la toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("capsulaActualizada", capsula);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        tvCapsuleTitle = findViewById(R.id.tvCapsuleTitle);
        tvCapsuleDescription = findViewById(R.id.tvCapsuleDescription);


        capsuleTitle = capsula.getTitulo();
        capsuleDescription = capsula.getDescripcion();

        // Actualizar la UI de título y descripción
        if (capsuleTitle != null && !capsuleTitle.isEmpty()) {
            tvCapsuleTitle.setText(capsuleTitle);
        }

        if (capsuleDescription != null && !capsuleDescription.isEmpty()) {
            tvCapsuleDescription.setText(capsuleDescription);
        }

        // Obtener imágenes y otros datos del Intent
        ArrayList<String> uriStrings = getIntent().getStringArrayListExtra("imagenes");
        List<Uri> images = new ArrayList<>();
        if (uriStrings != null) {
            for (String uriString : uriStrings) {
                images.add(Uri.parse(uriString));
            }
        }

        latitude = capsula.getLatitud();
        longitude = capsula.getLongitud();

        // Configurar RecyclerView con las imágenes
        RecyclerView rvImagenes = findViewById(R.id.rvImagenes);
        ImagenAdapter adapter = new ImagenAdapter();
        adapter.agregarImagenes(images);
        rvImagenes.setAdapter(adapter);
        rvImagenes.setLayoutManager(new LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
        ));

        // Configurar el MapView
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap gMap) {
        googleMap = gMap;
        LatLng ubicacion = new LatLng(latitude, longitude);
        googleMap.addMarker(new MarkerOptions().position(ubicacion));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 15));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail_capsule, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            showEditDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showEditDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_edit_capsule, null);

        final EditText etEditTitle = dialogView.findViewById(R.id.etEditTitle);
        final EditText etEditDescription = dialogView.findViewById(R.id.etEditDescription);

        etEditTitle.setText(capsuleTitle);
        etEditDescription.setText(capsuleDescription);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Editar cápsula")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialogInterface, i) -> {
                    String nuevoTitulo = etEditTitle.getText().toString().trim();
                    String nuevaDescripcion = etEditDescription.getText().toString().trim();

                    if (!nuevoTitulo.isEmpty()) {
                        capsuleTitle = nuevoTitulo;
                        capsuleDescription = nuevaDescripcion;

                        tvCapsuleTitle.setText(capsuleTitle);
                        tvCapsuleDescription.setText(capsuleDescription);

                        capsula.setTitulo(capsuleTitle);
                        capsula.setDescripcion(capsuleDescription);

                        new Thread(() -> {
                            db.capsulaDao().actualizarCapsula(capsula);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Cápsula actualizada", Toast.LENGTH_SHORT).show();

                                // Devolver los datos actualizados a la actividad anterior
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("capsulaActualizada", capsula);
                                setResult(RESULT_OK, resultIntent);
                            });
                        }).start();
                    } else {
                        Toast.makeText(this, "El título no puede estar vacío", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
