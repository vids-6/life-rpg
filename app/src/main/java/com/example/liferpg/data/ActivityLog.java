package com.example.liferpg.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Every reward ever granted. The UNIQUE index on refKey is the duplicate-reward guard:
 * a second insert for the same quest/day, focus session or workout session is ignored.
 */
@Entity(tableName = "activity_log",
        indices = {@Index(value = "refKey", unique = true), @Index("dateKey"), @Index("type")})
public class ActivityLog {
    @PrimaryKey(autoGenerate = true)
    public long id;
    /** QUEST / FOCUS / WORKOUT. */
    @NonNull
    public String type = "";
    @NonNull
    public String refKey = "";
    /** Quest id for QUEST rows, else 0. */
    public long refId;
    @NonNull
    public String title = "";
    /** Category enum name, null for focus sessions. */
    public String category;
    public int xp;
    public long timestamp;
    /** yyyy-MM-dd. */
    @NonNull
    public String dateKey = "";
    public int durationMin;
    /** Gym name for workouts. */
    public String detail;
    /** WorkoutType enum name for workouts. */
    public String subType;
}
