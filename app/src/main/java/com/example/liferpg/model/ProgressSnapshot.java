package com.example.liferpg.model;

/** Immutable view of the player's real progress, used to evaluate achievements. */
public final class ProgressSnapshot {
    public final int questCount;
    public final int focusCount;
    public final int workoutCount;
    public final int bestStreak;
    public final int level;

    public ProgressSnapshot(int questCount, int focusCount, int workoutCount, int bestStreak, int level) {
        this.questCount = questCount;
        this.focusCount = focusCount;
        this.workoutCount = workoutCount;
        this.bestStreak = bestStreak;
        this.level = level;
    }
}
