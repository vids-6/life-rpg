package com.example.liferpg.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AchievementDao {
    /** Returns -1 if already unlocked. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(UnlockedAchievement achievement);

    @Query("SELECT * FROM unlocked_achievements ORDER BY unlockedAt DESC")
    List<UnlockedAchievement> all();

    @Query("DELETE FROM unlocked_achievements")
    void clear();
}
