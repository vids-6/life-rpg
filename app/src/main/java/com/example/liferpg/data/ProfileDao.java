package com.example.liferpg.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ProfileDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    PlayerProfile get();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(PlayerProfile profile);
}
