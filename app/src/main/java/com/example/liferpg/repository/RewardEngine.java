package com.example.liferpg.repository;

import java.util.ArrayList;
import java.util.List;

import com.example.liferpg.data.ActivityLog;
import com.example.liferpg.data.AppDatabase;
import com.example.liferpg.data.PlayerProfile;
import com.example.liferpg.data.UnlockedAchievement;
import com.example.liferpg.logic.AchievementEngine;
import com.example.liferpg.logic.AttributeGain;
import com.example.liferpg.logic.LevelSystem;
import com.example.liferpg.logic.StreakCalculator;
import com.example.liferpg.model.Achievement;
import com.example.liferpg.model.ActivityType;
import com.example.liferpg.model.Attribute;
import com.example.liferpg.model.ProgressSnapshot;
import com.example.liferpg.model.RewardResult;

/**
 * The single place where XP is granted. Everything happens in ONE database transaction:
 * activity-log insert (unique refKey = duplicate guard) + XP + attributes + streak + achievements.
 */
public class RewardEngine {
    private final AppDatabase db;

    public RewardEngine(AppDatabase db) {
        this.db = db;
    }

    /** Blocking. Call from a background thread. Safe to call inside an outer transaction. */
    public RewardResult award(final ActivityLog log, final AttributeGain gain) {
        return db.runInTransaction(() -> awardInTransaction(log, gain));
    }

    private RewardResult awardInTransaction(ActivityLog log, AttributeGain gain) {
        PlayerProfile profile = db.profileDao().get();
        if (profile == null) return RewardResult.rejected("No player profile found.");
        if (log.xp <= 0) return RewardResult.rejected("Nothing to reward.");

        long rowId = db.activityDao().insert(log);
        if (rowId == -1L) {
            return RewardResult.rejected("Reward already claimed.");
        }

        int oldLevel = LevelSystem.levelForXp(profile.totalXp);
        profile.totalXp += log.xp;
        for (Attribute a : Attribute.values()) {
            profile.addAttribute(a, gain.get(a));
        }
        profile.currentStreak = StreakCalculator.nextStreak(profile.lastActiveDate, log.dateKey, profile.currentStreak);
        profile.bestStreak = Math.max(profile.bestStreak, profile.currentStreak);
        profile.lastActiveDate = log.dateKey;
        db.profileDao().upsert(profile);

        int newLevel = LevelSystem.levelForXp(profile.totalXp);
        ProgressSnapshot snapshot = new ProgressSnapshot(
                db.activityDao().count(ActivityType.QUEST),
                db.activityDao().count(ActivityType.FOCUS),
                db.activityDao().count(ActivityType.WORKOUT),
                profile.bestStreak,
                newLevel);

        List<Achievement> unlocked = new ArrayList<>();
        for (Achievement a : AchievementEngine.evaluate(snapshot)) {
            long id = db.achievementDao().insert(new UnlockedAchievement(a.name(), log.timestamp));
            if (id != -1L) unlocked.add(a);
        }
        return RewardResult.granted(log.xp, oldLevel, newLevel, unlocked);
    }
}
