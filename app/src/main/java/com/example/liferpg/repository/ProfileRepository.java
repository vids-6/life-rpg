package com.example.liferpg.repository;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import com.example.liferpg.data.AppDatabase;
import com.example.liferpg.data.PlayerProfile;
import com.example.liferpg.data.Quest;
import com.example.liferpg.data.UnlockedAchievement;
import com.example.liferpg.logic.DateUtils;
import com.example.liferpg.logic.StreakCalculator;
import com.example.liferpg.model.Achievement;
import com.example.liferpg.model.ActivityType;
import com.example.liferpg.util.AppPrefs;
import com.example.liferpg.util.FocusStateStore;
import com.example.liferpg.util.WorkoutSessionStore;

public class ProfileRepository {
    public static final int MAX_NAME_LENGTH = 24;

    /** Everything the dashboard needs, loaded in one go. */
    public static class DashboardData {
        public PlayerProfile profile;
        public int effectiveStreak;
        public int dailyDone;
        public int dailyTotal;
        public List<UnlockedAchievement> recentAchievements = new ArrayList<>();
        /** Today's quests (daily, due today, completed today); unfinished first. */
        public List<Quest> todayQuests = new ArrayList<>();
        public int questsCompleted;
        public int focusSessions;
        public int workouts;
        public int achievementsUnlocked;
        public int achievementsTotal = Achievement.values().length;
    }

    private final Context context;
    private final AppDatabase db;

    public ProfileRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.get(this.context);
    }

    public PlayerProfile get() {
        return db.profileDao().get();
    }

    private static String validateName(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("Please enter a hero name.");
        if (n.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Name can be at most " + MAX_NAME_LENGTH + " characters.");
        }
        return n;
    }

    public void createProfile(String name, int avatarIndex) {
        final String n = validateName(name);
        PlayerProfile p = new PlayerProfile();
        p.name = n;
        p.avatarIndex = avatarIndex;
        p.createdAt = System.currentTimeMillis();
        db.profileDao().upsert(p);
        new AppPrefs(context).setOnboarded(true);
    }

    public void updateProfile(String name, int avatarIndex) {
        final String n = validateName(name);
        db.runInTransaction(() -> {
            PlayerProfile p = db.profileDao().get();
            if (p == null) {
                p = new PlayerProfile();
                p.createdAt = System.currentTimeMillis();
            }
            p.name = n;
            p.avatarIndex = avatarIndex;
            db.profileDao().upsert(p);
        });
    }

    public DashboardData loadDashboard() {
        DashboardData d = new DashboardData();
        d.profile = db.profileDao().get();
        if (d.profile == null) return d;
        d.effectiveStreak = StreakCalculator.effectiveStreak(
                d.profile.lastActiveDate, DateUtils.todayKey(), d.profile.currentStreak);
        d.todayQuests = new QuestRepository(context).todayQuests();
        String today = DateUtils.todayKey();
        for (Quest q : d.todayQuests) {
            d.dailyTotal++;
            if (q.isDoneToday(today)) d.dailyDone++;
        }
        List<UnlockedAchievement> all = db.achievementDao().all();
        d.achievementsUnlocked = all.size();
        d.recentAchievements = all.size() > 3 ? new ArrayList<>(all.subList(0, 3)) : all;
        d.questsCompleted = db.activityDao().count(ActivityType.QUEST);
        d.focusSessions = db.activityDao().count(ActivityType.FOCUS);
        d.workouts = db.activityDao().count(ActivityType.WORKOUT);
        return d;
    }

    public List<UnlockedAchievement> unlockedAchievements() {
        return db.achievementDao().all();
    }

    /** Wipes XP, attributes, streaks, history, achievements and quest completion. Keeps name/avatar, quests, saved gyms. */
    public void resetProgress() {
        db.runInTransaction(() -> {
            db.activityDao().clear();
            db.achievementDao().clear();
            db.questDao().resetCompletion();
            PlayerProfile p = db.profileDao().get();
            if (p != null) {
                p.totalXp = 0;
                p.intelligence = 0;
                p.strength = 0;
                p.discipline = 0;
                p.creativity = 0;
                p.vitality = 0;
                p.currentStreak = 0;
                p.bestStreak = 0;
                p.lastActiveDate = null;
                db.profileDao().upsert(p);
            }
        });
        new FocusStateStore(context).clear();
        new WorkoutSessionStore(context).clear();
    }
}
