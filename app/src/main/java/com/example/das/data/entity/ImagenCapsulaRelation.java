package com.example.das.data.entity;
import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;
public class ImagenCapsulaRelation {
    @Embedded
    public Capsula capsula;

    @Relation(
            parentColumn = "id",
            entityColumn = "capsulaId"
    )
    public List<Imagen> imagenes;
}
