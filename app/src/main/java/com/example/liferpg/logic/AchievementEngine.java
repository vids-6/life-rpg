package com.example.liferpg.logic;

import java.util.ArrayList;
import java.util.List;

import com.example.liferpg.model.Achievement;
import com.example.liferpg.model.ProgressSnapshot;

/** Decides which achievements are earned from real progress. */
public final class AchievementEngine {
    private AchievementEngine() { }

    /** Current progress value towards the achievement's target. */
    public static int current(Achievement a, ProgressSnapshot s) {
        switch (a) {
            case FIRST_QUEST:
            case TEN_QUESTS:
                return s.questCount;
            case FIRST_FOCUS:
                return s.focusCount;
            case STREAK_5:
                return s.bestStreak;
            case LEVEL_5:
                return s.level;
            default:
                return s.workoutCount;
        }
    }

    public static boolean isEarned(Achievement a, ProgressSnapshot s) {
        return current(a, s) >= a.target;
    }

    public static List<Achievement> evaluate(ProgressSnapshot s) {
        List<Achievement> earned = new ArrayList<>();
        for (Achievement a : Achievement.values()) {
            if (isEarned(a, s)) earned.add(a);
        }
        return earned;
    }
}
