package com.example.liferpg.repository;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.liferpg.data.ActivityLog;
import com.example.liferpg.data.AppDatabase;
import com.example.liferpg.data.CategoryStat;
import com.example.liferpg.data.DayXp;
import com.example.liferpg.data.PlayerProfile;
import com.example.liferpg.logic.DateUtils;
import com.example.liferpg.logic.LevelSystem;
import com.example.liferpg.logic.StreakCalculator;
import com.example.liferpg.model.ActivityType;

public class StatsRepository {

    public static class StatsData {
        public long totalXp;
        public int level;
        public int questCount;
        public int workoutCount;
        public int focusCount;
        public int focusMinutes;
        public int workoutMinutes;
        public int todayXp;
        public int weekXp;
        public int currentStreak;
        public int bestStreak;
        public String[] dayLabels = new String[7];
        public int[] dayXp = new int[7];
        public List<CategoryStat> categories = new ArrayList<>();
        public List<ActivityLog> recent = new ArrayList<>();
    }

    private final AppDatabase db;

    public StatsRepository(Context context) {
        this.db = AppDatabase.get(context.getApplicationContext());
    }

    public StatsData load() {
        StatsData s = new StatsData();
        PlayerProfile p = db.profileDao().get();
        String today = DateUtils.todayKey();
        if (p != null) {
            s.totalXp = p.totalXp;
            s.level = LevelSystem.levelForXp(p.totalXp);
            s.currentStreak = StreakCalculator.effectiveStreak(p.lastActiveDate, today, p.currentStreak);
            s.bestStreak = p.bestStreak;
        } else {
            s.level = 1;
        }
        s.questCount = db.activityDao().count(ActivityType.QUEST);
        s.workoutCount = db.activityDao().count(ActivityType.WORKOUT);
        s.focusCount = db.activityDao().count(ActivityType.FOCUS);
        s.focusMinutes = db.activityDao().totalMinutes(ActivityType.FOCUS);
        s.workoutMinutes = db.activityDao().totalMinutes(ActivityType.WORKOUT);
        s.todayXp = db.activityDao().xpBetween(today, today);
        s.weekXp = db.activityDao().xpBetween(DateUtils.mondayOfWeek(today), today);

        String from = DateUtils.plusDays(today, -6);
        Map<String, Integer> byDay = new HashMap<>();
        for (DayXp d : db.activityDao().xpByDay(from, today)) {
            byDay.put(d.dateKey, d.xp);
        }
        for (int i = 0; i < 7; i++) {
            String key = DateUtils.plusDays(from, i);
            s.dayLabels[i] = DateUtils.weekdayShort(key);
            Integer v = byDay.get(key);
            s.dayXp[i] = v == null ? 0 : v;
        }
        s.categories = db.activityDao().categoryStats();
        s.recent = db.activityDao().recent(25);
        return s;
    }
}
