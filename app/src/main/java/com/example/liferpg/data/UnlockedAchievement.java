package com.example.liferpg.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "unlocked_achievements")
public class UnlockedAchievement {
    @PrimaryKey
    @NonNull
    public String key = "";
    public long unlockedAt;

    public UnlockedAchievement() { }

    @Ignore
    public UnlockedAchievement(@NonNull String key, long unlockedAt) {
        this.key = key;
        this.unlockedAt = unlockedAt;
    }
}
