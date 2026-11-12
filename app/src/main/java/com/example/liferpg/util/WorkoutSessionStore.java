package com.example.liferpg.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.liferpg.model.WorkoutType;

/** Persists an in-progress workout (stopwatch start time, gym, type). */
public class WorkoutSessionStore {
    private static final String FILE = "liferpg_workout";
    private final SharedPreferences sp;

    public static class Session {
        public String sessionId;
        public long startMs;
        public String gymName;
        public String gymId;
        public WorkoutType type;
    }

    public WorkoutSessionStore(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Returns null when no workout is in progress. */
    public Session load() {
        if (!sp.getBoolean("active", false)) return null;
        Session s = new Session();
        s.sessionId = sp.getString("session", null);
        s.startMs = sp.getLong("start", 0L);
        s.gymName = sp.getString("gymName", "");
        s.gymId = sp.getString("gymId", "");
        s.type = WorkoutType.fromName(sp.getString("type", null));
        if (s.sessionId == null || s.startMs <= 0L) {
            clear();
            return null;
        }
        return s;
    }

    public void save(Session s) {
        sp.edit().putBoolean("active", true)
                .putString("session", s.sessionId)
                .putLong("start", s.startMs)
                .putString("gymName", s.gymName)
                .putString("gymId", s.gymId)
                .putString("type", s.type.name())
                .apply();
    }

    public void clear() {
        sp.edit().clear().apply();
    }
}
