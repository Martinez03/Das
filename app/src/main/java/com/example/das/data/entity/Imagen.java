package com.example.das.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(
        tableName = "imagenes",
        foreignKeys = @ForeignKey(
                entity = Capsula.class,
                parentColumns = "id",
                childColumns = "capsulaId",
                onDelete = ForeignKey.CASCADE
        )
)
public class Imagen implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id; // Entero autoincremental
    private int capsulaId;
    private byte[] foto;
    private String titulo;

    // Constructor
    public Imagen(int capsulaId, byte[] foto) {
        this.capsulaId = capsulaId;
        this.foto = foto;
    }

    // Getters y setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCapsulaId() { return capsulaId; }
    public void setCapsulaId(int capsulaId) { this.capsulaId = capsulaId; }

    public byte[] getFoto() {
        return foto;
    }

    public void setFoto( byte[] foto) {
        this.foto = foto;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
}
