package com.example.liferpg.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GymDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(SavedGym gym);

    @Query("DELETE FROM saved_gyms WHERE placeId = :placeId")
    void remove(String placeId);

    @Query("SELECT * FROM saved_gyms ORDER BY savedAt DESC")
    List<SavedGym> all();

    @Query("SELECT placeId FROM saved_gyms")
    List<String> ids();
}
