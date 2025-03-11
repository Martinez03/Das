package com.example.das.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.das.R;

import java.util.ArrayList;
import java.util.List;

public class ImagenAdapter extends RecyclerView.Adapter<ImagenAdapter.ViewHolder> {

    private List<Uri> imagenes = new ArrayList<>();

    public void agregarImagenes(List<Uri> nuevasImagenes) {
        int startPosition = imagenes.size();
        imagenes.addAll(nuevasImagenes);
        notifyItemRangeInserted(startPosition, nuevasImagenes.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_imagen, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = imagenes.get(position);
        Glide.with(holder.itemView.getContext())
                .load(uri)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imagenes.size();
    }

    public List<Uri> getImagenes() {
        return imagenes;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}