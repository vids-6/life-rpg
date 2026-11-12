package com.example.liferpg.repository;

import android.content.Context;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.liferpg.data.AppDatabase;
import com.example.liferpg.data.SavedGym;
import com.example.liferpg.model.Gym;

public class GymRepository {
    private final AppDatabase db;

    public GymRepository(Context context) {
        this.db = AppDatabase.get(context.getApplicationContext());
    }

    public List<SavedGym> getSaved() {
        return db.gymDao().all();
    }

    public Set<String> savedIds() {
        return new HashSet<>(db.gymDao().ids());
    }

    public void save(Gym gym) {
        if (gym.placeId == null || gym.placeId.isEmpty()) return;
        SavedGym s = new SavedGym();
        // placeId is the primary key (e.g. "osm:node:123"); REPLACE on conflict means saving twice never duplicates.
        s.placeId = gym.placeId;
        s.name = gym.name;
        s.address = gym.address;
        s.lat = gym.lat;
        s.lng = gym.lng;
        s.rating = gym.rating;
        s.savedAt = System.currentTimeMillis();
        db.gymDao().save(s);
    }

    public void remove(String placeId) {
        db.gymDao().remove(placeId);
    }

    public static Gym toGym(SavedGym s) {
        Gym g = new Gym();
        g.placeId = s.placeId;
        g.name = s.name;
        g.address = s.address;
        g.lat = s.lat;
        g.lng = s.lng;
        g.rating = s.rating;
        return g;
    }
}
