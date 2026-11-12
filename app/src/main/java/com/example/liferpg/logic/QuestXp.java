package com.example.liferpg.logic;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.example.liferpg.model.Category;
import com.example.liferpg.model.Difficulty;

/**
 * Quest reward rules (pure Java, unit tested):
 * base XP = difficulty XP x duration multiplier; late completion keeps only a share of it.
 */
public final class QuestXp {
    private QuestXp() { }

    public static final long HOUR_MS = 60L * 60L * 1000L;

    /** Predefined duration choices in minutes. Legacy categories use the Custom list. */
    public static int[] durationOptions(Category c) {
        switch (c) {
            case STUDY: return new int[]{15, 30, 60, 120, 180};
            case FITNESS: return new int[]{15, 30, 45, 60, 90};
            case PERSONAL: return new int[]{10, 20, 30, 60, 120};
            case PROJECTS: return new int[]{30, 60, 120, 180, 300};
            case DAILY_LIFE: return new int[]{5, 15, 30, 60, 120};
            default: return new int[]{15, 30, 60, 120, 180};
        }
    }

    /** 15 -> "15m", 60 -> "1h", 90 -> "90m", 180 -> "3h". */
    public static String formatDuration(int minutes) {
        if (minutes <= 0) return "";
        if (minutes % 60 == 0) return (minutes / 60) + "h";
        return minutes + "m";
    }

    /** Percent (x100): <=15m 75, 16-30m 100, 31-60m 150, 61-120m 200, longer 250. */
    public static int durationMultiplierPercent(int minutes) {
        if (minutes <= 15) return 75;
        if (minutes <= 30) return 100;
        if (minutes <= 60) return 150;
        if (minutes <= 120) return 200;
        return 250;
    }

    /** difficulty XP x duration multiplier, rounded to a whole number (half up). */
    public static int baseXp(Difficulty d, int minutes) {
        return (d.defaultXp * durationMultiplierPercent(minutes) + 50) / 100;
    }

    /** Suggested due date (start of that day, epoch millis): <=30m today, <=60m +1 day, <=120m +2, longer +3. */
    public static long suggestedDueDate(int minutes, LocalDate today, ZoneId zone) {
        int days;
        if (minutes <= 30) days = 0;
        else if (minutes <= 60) days = 1;
        else if (minutes <= 120) days = 2;
        else days = 3;
        return today.plusDays(days).atStartOfDay(zone).toInstant().toEpochMilli();
    }

    public static long suggestedDueDate(int minutes) {
        return suggestedDueDate(minutes, LocalDate.now(), ZoneId.systemDefault());
    }

    /** The deadline is the END of the due day (last millisecond before the next midnight). */
    public static long deadlineMillis(long dueStartOfDay, ZoneId zone) {
        ZonedDateTime day = Instant.ofEpochMilli(dueStartOfDay).atZone(zone);
        return day.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1;
    }

    /** Share of the XP that is kept: 100 on time, 80 up to 24h late, 60 up to 48h, 40 up to 72h, else 25. */
    public static int latePercent(long dueStartOfDay, long nowMillis, ZoneId zone) {
        if (dueStartOfDay <= 0) return 100;
        long late = nowMillis - deadlineMillis(dueStartOfDay, zone);
        if (late <= 0) return 100;
        if (late <= 24 * HOUR_MS) return 80;
        if (late <= 48 * HOUR_MS) return 60;
        if (late <= 72 * HOUR_MS) return 40;
        return 25;
    }

    public static int latePercent(long dueStartOfDay, long nowMillis) {
        return latePercent(dueStartOfDay, nowMillis, ZoneId.systemDefault());
    }

    /** XP actually granted: base x late share, rounded half up, never below 1. */
    public static int earnedXp(int baseXp, int percent) {
        return Math.max(1, (baseXp * percent + 50) / 100);
    }

    public static int earnedXp(int baseXp, long dueStartOfDay, long nowMillis) {
        return earnedXp(baseXp, latePercent(dueStartOfDay, nowMillis));
    }
}
