package com.example.das.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.das.data.entity.Capsula;
import com.example.das.data.entity.Imagen;
import com.example.das.data.entity.ImagenCapsulaRelation;

import java.util.List;

@Dao
public interface CapsulaDao {
    // Operaciones para Capsula
    @Insert
    long insertarCapsula(Capsula capsula);

    @Query("SELECT * FROM capsulas")
    List<Capsula> obtenerCapsulas();
    @Update
    void actualizarCapsula(Capsula capsula);

    @Delete
    void eliminarCapsula(Capsula capsula);

    // Operaciones para Imagen
    @Insert
    void insertarImagen(Imagen imagen);

    @Delete
    void eliminarImagen(Imagen imagen);

    @Query("SELECT * FROM imagenes WHERE capsulaId = :capsulaId")
    List<Imagen> obtenerImagenesPorCapsula(int capsulaId);

    // Consulta para obtener UNA cápsula con imágenes
    @Transaction
    @Query("SELECT * FROM capsulas WHERE id = :capsulaId")
    ImagenCapsulaRelation obtenerCapsulaConImagenes(int capsulaId);

    // Consulta para obtener TODAS las cápsulas con imágenes
    @Transaction
    @Query("SELECT * FROM capsulas")
    List<ImagenCapsulaRelation> obtenerTodasCapsulasConImagenes();
}
