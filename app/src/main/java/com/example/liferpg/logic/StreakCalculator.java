package com.example.liferpg.logic;

public final class StreakCalculator {
    private StreakCalculator() { }

    /** Streak after earning a reward on todayKey. */
    public static int nextStreak(String lastActiveKey, String todayKey, int currentStreak) {
        if (lastActiveKey == null) return 1;
        if (lastActiveKey.equals(todayKey)) return Math.max(1, currentStreak);
        return DateUtils.daysBetween(lastActiveKey, todayKey) == 1L ? currentStreak + 1 : 1;
    }

    /** Streak to display today: a streak that was not extended yesterday or today has lapsed. */
    public static int effectiveStreak(String lastActiveKey, String todayKey, int storedStreak) {
        if (lastActiveKey == null) return 0;
        long gap = DateUtils.daysBetween(lastActiveKey, todayKey);
        return (gap == 0L || gap == 1L) ? storedStreak : 0;
    }
}
