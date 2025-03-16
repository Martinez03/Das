package com.example.das.activities;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.bumptech.glide.Glide;
import com.example.das.R;
import com.example.das.adapter.ImagenAdapter;
import com.example.das.data.database.AppDatabase;
import com.example.das.data.entity.Capsula;
import com.example.das.data.entity.Imagen;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DetailCapsuleActivity extends AppCompatActivity implements OnMapReadyCallback {
    private AppDatabase db;
    private MapView mapView;
    private GoogleMap googleMap;
    private double latitude;
    private double longitude;
    private Capsula capsula;
    private List<Imagen> imagenes;
    private String capsuleTitle;
    private String capsuleDescription;
    // Variables para el diálogo de edición
    private AlertDialog editDialog;
    private boolean isEditDialogShowing = false;
    private String savedEditTitle = null;
    private String savedEditDescription = null;


    private androidx.appcompat.widget.AppCompatTextView tvCapsuleTitle;
    private androidx.appcompat.widget.AppCompatTextView tvCapsuleDescription;


    /**
     *
     * Inicializa el activity con los datos de la capsula clickada
     */
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
        imagenes = (List<Imagen>) getIntent().getSerializableExtra("imagenes");
        List<Uri> images = new ArrayList<>();
        if (imagenes != null) {
            for (Imagen uriString : imagenes) {
                images.add(Uri.parse(uriString.getUrl()));
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

        // Restaurar el estado del diálogo de edición si estaba abierto
        if (savedInstanceState != null && savedInstanceState.getBoolean("isEditDialogShowing", false)) {
            savedEditTitle = savedInstanceState.getString("savedEditTitle");
            savedEditDescription = savedInstanceState.getString("savedEditDescription");
            showEditDialog(savedEditTitle, savedEditDescription);
        }
    }

    /**
     *
     * Opciones del toolbar editar, exportar, eliminar
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            showEditDialog(null, null);
            return true;
        } else if (item.getItemId() == R.id.action_delete) {
            confirmarEliminacion();
            return true;
        } else if (item.getItemId() == R.id.action_export) {
            exportarATXT();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportarATXT() {
        String nombreArchivo = "capsula_" + capsula.getId() + ".txt";
        StringBuilder contenido = new StringBuilder()
                .append("Título: ").append(capsula.getTitulo()).append("\n")
                .append("Descripción: ").append(capsula.getDescripcion()).append("\n")
                .append("Coordenadas: ").append(capsula.getLatitud()).append(", ").append(capsula.getLongitud()).append("\n")
                .append("Imágenes:\n");

        // Obtener URLs de imágenes si es necesario
        List<Imagen> imagenes = db.capsulaDao().obtenerImagenesPorCapsula(capsula.getId());
        for (Imagen imagen : imagenes) {
            contenido.append("- ").append(imagen.getUrl()).append("\n");
        }

        // Guardar el archivo en la carpeta de descargas
        guardarArchivoEnDescargas(nombreArchivo, contenido.toString());
    }


    /**
     *
     * Se guarda el txt en la carpeta descargas
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void guardarArchivoEnDescargas(String nombreArchivo, String contenido) {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, nombreArchivo);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(contenido.getBytes());
                    Toast.makeText(this, "Archivo guardado en Descargas", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error al guardar el archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Error al crear el archivo", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     *
     * Dialog para confirmar la eliminacion de la capsula
     */
    private void confirmarEliminacion() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar cápsula")
                .setMessage("¿Estás seguro de eliminar esta cápsula?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarCapsula())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     *
     * Se elimina la capsula de la base de datos
     */
    private void eliminarCapsula() {
        new Thread(() -> {
            db.capsulaDao().eliminarCapsula(capsula);
            runOnUiThread(() -> {
                Toast.makeText(this, "Cápsula eliminada", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("capsulaActualizada", capsula);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        }).start();
    }


    /**
     *
     * Mapa
     */
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


    /**
     *
     * Se ve el dialog para editar la capsula, si la pantalla cambia los datos persisten
     */
    private void showEditDialog(String initialTitle, String initialDescription) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_edit_capsule, null);

        final EditText etEditTitle = dialogView.findViewById(R.id.etEditTitle);
        final EditText etEditDescription = dialogView.findViewById(R.id.etEditDescription);

        // Asigna los valores iniciales: si se pasan desde el estado guardado, se usan; de lo contrario, se usan los actuales
        if (initialTitle != null) {
            etEditTitle.setText(initialTitle);
        } else {
            etEditTitle.setText(capsuleTitle);
        }

        if (initialDescription != null) {
            etEditDescription.setText(initialDescription);
        } else {
            etEditDescription.setText(capsuleDescription);
        }

        editDialog = new AlertDialog.Builder(this)
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

        // Cuando se cierre el diálogo, se actualiza la bandera
        editDialog.setOnDismissListener(dialog -> isEditDialogShowing = false);

        isEditDialogShowing = true;
        editDialog.show();
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

    /**
     *
     * Por si se cambia la pantalla siguen los datos del dialog si este esta abierto
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);

        if (editDialog != null && editDialog.isShowing()) {
            EditText etEditTitle = editDialog.findViewById(R.id.etEditTitle);
            EditText etEditDescription = editDialog.findViewById(R.id.etEditDescription);
            if (etEditTitle != null && etEditDescription != null) {
                outState.putBoolean("isEditDialogShowing", true);
                outState.putString("savedEditTitle", etEditTitle.getText().toString());
                outState.putString("savedEditDescription", etEditDescription.getText().toString());
            }
        }
    }

}
