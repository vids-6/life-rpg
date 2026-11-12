package com.example.liferpg.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.liferpg.logic.FocusTimerLogic;

/** Persists the focus timer so it survives backgrounding, activity recreation and process death. */
public class FocusStateStore {
    private static final String FILE = "liferpg_focus";
    private final SharedPreferences sp;

    public FocusStateStore(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public FocusTimerLogic load() {
        FocusTimerLogic logic = new FocusTimerLogic();
        FocusTimerLogic.State state;
        try {
            state = FocusTimerLogic.State.valueOf(sp.getString("state", "IDLE"));
        } catch (IllegalArgumentException e) {
            state = FocusTimerLogic.State.IDLE;
        }
        logic.restore(state, sp.getInt("duration", 25), sp.getLong("endAt", 0L),
                sp.getLong("remaining", 0L), sp.getString("session", null));
        return logic;
    }

    public void save(FocusTimerLogic logic) {
        sp.edit()
                .putString("state", logic.getState().name())
                .putInt("duration", logic.getDurationMin())
                .putLong("endAt", logic.getEndAtMs())
                .putLong("remaining", logic.getStoredRemainingMs())
                .putString("session", logic.getSessionId())
                .apply();
    }

    public void clear() {
        sp.edit().clear().apply();
    }
}
