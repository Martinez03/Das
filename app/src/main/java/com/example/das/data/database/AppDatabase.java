package com.example.das.data.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.das.data.dao.CapsulaDao;
import com.example.das.data.entity.Capsula;
import com.example.das.data.entity.Imagen;
@Database(entities = {Capsula.class, Imagen.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CapsulaDao capsulaDao();
}