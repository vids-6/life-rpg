package com.example.liferpg.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Outcome of trying to grant a reward. awarded=false means nothing was changed. */
public final class RewardResult {
    public final boolean awarded;
    public final int xpGained;
    public final int oldLevel;
    public final int newLevel;
    public final List<Achievement> achievements;
    public final String note;
    /** XP before a late penalty (equals xpGained when on time) and the share that was kept (100 = on time). */
    public final int baseXp;
    public final int latePercent;

    private RewardResult(boolean awarded, int xpGained, int oldLevel, int newLevel,
                         List<Achievement> achievements, String note, int baseXp, int latePercent) {
        this.awarded = awarded;
        this.xpGained = xpGained;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.achievements = achievements;
        this.note = note;
        this.baseXp = baseXp;
        this.latePercent = latePercent;
    }

    public static RewardResult granted(int xp, int oldLevel, int newLevel, List<Achievement> achievements) {
        return new RewardResult(true, xp, oldLevel, newLevel, new ArrayList<>(achievements), "", xp, 100);
    }

    public static RewardResult rejected(String note) {
        return new RewardResult(false, 0, 0, 0, Collections.<Achievement>emptyList(), note, 0, 100);
    }

    /** Same result, annotated with the late penalty that was applied. */
    public RewardResult withLate(int baseXp, int latePercent) {
        return new RewardResult(awarded, xpGained, oldLevel, newLevel, achievements, note, baseXp, latePercent);
    }

    public boolean wasLate() {
        return awarded && latePercent < 100;
    }

    /** "+48 XP (80% of 60: completed late)" or just "+60 XP". */
    public String xpText() {
        if (!wasLate()) return "+" + xpGained + " XP";
        return "+" + xpGained + " XP (" + latePercent + "% of " + baseXp + ": late)";
    }

    public boolean leveledUp() {
        return awarded && newLevel > oldLevel;
    }

    /** Human readable multi-line summary for toasts/dialogs. */
    public String summary() {
        if (!awarded) return note;
        StringBuilder sb = new StringBuilder(xpText());
        if (leveledUp()) sb.append("\nLEVEL UP! You are now level ").append(newLevel);
        for (Achievement a : achievements) {
            sb.append("\n").append(a.emoji).append(" Achievement unlocked: ").append(a.title);
        }
        return sb.toString();
    }
}
