package com.example.das.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "imagenes",
        foreignKeys = @ForeignKey(
                entity = Capsula.class,
                parentColumns = "id",
                childColumns = "capsulaId",
                onDelete = ForeignKey.CASCADE
        )
)
public class Imagen {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int capsulaId; // Relación con Capsula
    private String url;

    // Constructor
    public Imagen(int capsulaId, String url) {
        this.capsulaId = capsulaId;
        this.url = url;
    }

    // Getters y setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCapsulaId() { return capsulaId; }
    public void setCapsulaId(int capsulaId) { this.capsulaId = capsulaId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
