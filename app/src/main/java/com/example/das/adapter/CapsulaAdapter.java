package com.example.das.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.das.R;
import com.example.das.data.entity.Capsula;
import com.example.das.data.entity.Imagen;
import com.example.das.data.entity.ImagenCapsulaRelation;

import java.util.ArrayList;
import java.util.List;

public class CapsulaAdapter extends RecyclerView.Adapter<CapsulaAdapter.ViewHolder> {

    private List<ImagenCapsulaRelation> listaCapsulas;
    private final OnCapsulaClickListener listener;

    public interface OnCapsulaClickListener {
        void onCapsulaClick(List<String> imagenes, double latitud, double longitud);
    }

    public CapsulaAdapter(List<ImagenCapsulaRelation> listaCapsulas, OnCapsulaClickListener listener) {
        this.listaCapsulas = listaCapsulas;
        this.listener = listener;
    }

    public void actualizarDatos(List<ImagenCapsulaRelation> nuevosDatos) {
        listaCapsulas = nuevosDatos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_capsula, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImagenCapsulaRelation relacion = listaCapsulas.get(position);
        Capsula capsula = relacion.capsula;
        List<Imagen> imagenes = relacion.imagenes;

        holder.txtTitulo.setText(capsula.getTitulo());
        holder.txtDescripcion.setText(capsula.getDescripcion());

        if (!imagenes.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(Uri.parse(imagenes.get(0).getUrl()))
                    .into(holder.imgPreview);
        }
        // Configurar clic en el ítem
        holder.itemView.setOnClickListener(v -> {
            List<String> urls = new ArrayList<>();

            // Verificar null safety
            if (relacion.imagenes != null) {
                for (Imagen imagen : relacion.imagenes) {
                    if (imagen != null && imagen.getUrl() != null) {
                        urls.add(imagen.getUrl());
                    }
                }
            }

            // Verificar coordenadas válidas
            if (capsula.getLatitud() != 0 && capsula.getLongitud() != 0) {
                listener.onCapsulaClick(
                        urls,
                        capsula.getLatitud(),
                        capsula.getLongitud()
                );
            } else {
                Toast.makeText(holder.itemView.getContext(),
                        "Ubicación no disponible",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaCapsulas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitulo, txtDescripcion;
        ImageView imgPreview;

        ViewHolder(View itemView) {
            super(itemView);
            txtTitulo = itemView.findViewById(R.id.txtTitulo);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcion);
            imgPreview = itemView.findViewById(R.id.imgPreview);
        }
    }
}