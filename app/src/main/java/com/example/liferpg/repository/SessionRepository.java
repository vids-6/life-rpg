package com.example.liferpg.repository;

import android.content.Context;

import com.example.liferpg.data.ActivityLog;
import com.example.liferpg.data.AppDatabase;
import com.example.liferpg.logic.DateUtils;
import com.example.liferpg.logic.XpCalculator;
import com.example.liferpg.model.ActivityType;
import com.example.liferpg.model.Category;
import com.example.liferpg.model.RewardResult;
import com.example.liferpg.model.WorkoutType;

/** Rewards for focus sessions and gym workouts. Both are protected by a per-session unique key. */
public class SessionRepository {
    private final AppDatabase db;
    private final RewardEngine engine;

    public SessionRepository(Context context) {
        this.db = AppDatabase.get(context.getApplicationContext());
        this.engine = new RewardEngine(db);
    }

    public RewardResult completeFocus(String sessionId, int minutes) {
        if (sessionId == null || sessionId.isEmpty() || minutes <= 0) {
            return RewardResult.rejected("Invalid focus session.");
        }
        int xp = XpCalculator.focusXp(minutes);
        ActivityLog log = new ActivityLog();
        log.type = ActivityType.FOCUS;
        log.refKey = "F:" + sessionId;
        log.title = "Focus session (" + minutes + " min)";
        log.xp = xp;
        log.timestamp = System.currentTimeMillis();
        log.dateKey = DateUtils.todayKey();
        log.durationMin = minutes;
        return engine.award(log, XpCalculator.focusGain(xp));
    }

    public RewardResult completeWorkout(String sessionId, String gymName, WorkoutType type, int minutes) {
        if (sessionId == null || sessionId.isEmpty()) return RewardResult.rejected("Invalid workout session.");
        if (gymName == null || gymName.trim().isEmpty()) return RewardResult.rejected("A gym is required.");
        int xp = XpCalculator.workoutXp(type, minutes);
        if (xp <= 0) {
            return RewardResult.rejected("Workouts shorter than " + XpCalculator.MIN_WORKOUT_MINUTES
                    + " minutes do not earn XP.");
        }
        ActivityLog log = new ActivityLog();
        log.type = ActivityType.WORKOUT;
        log.refKey = "W:" + sessionId;
        log.title = type.label + " workout";
        log.category = Category.FITNESS.name();
        log.xp = xp;
        log.timestamp = System.currentTimeMillis();
        log.dateKey = DateUtils.todayKey();
        log.durationMin = minutes;
        log.detail = gymName.trim();
        log.subType = type.name();
        return engine.award(log, XpCalculator.workoutGain(type, xp));
    }

    /** {focusToday, focusTotal, focusMinutes}. */
    public int[] focusSummary() {
        String today = DateUtils.todayKey();
        return new int[]{
                db.activityDao().countBetween(ActivityType.FOCUS, today, today),
                db.activityDao().count(ActivityType.FOCUS),
                db.activityDao().totalMinutes(ActivityType.FOCUS)};
    }
}
