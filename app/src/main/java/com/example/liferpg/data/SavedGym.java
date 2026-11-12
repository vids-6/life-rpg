package com.example.liferpg.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "saved_gyms")
public class SavedGym {
    @PrimaryKey
    @NonNull
    public String placeId = "";
    @NonNull
    public String name = "";
    @NonNull
    public String address = "";
    public double lat;
    public double lng;
    /** -1 = no rating. */
    public double rating = -1;
    public long savedAt;
}
