package com.example.liferpg.logic;

/**
 * Progression formula. Going from level L to L+1 costs 100 + 50*(L-1) XP.
 * Cumulative XP needed to reach level n = 100*(n-1) + 25*(n-1)*(n-2).
 * Level is always derived from total XP, so carry-over after a level-up is automatic.
 */
public final class LevelSystem {
    public static final int MAX_LEVEL = 100;

    private LevelSystem() { }

    public static long xpToReach(int level) {
        if (level <= 1) return 0L;
        long n = level - 1L;
        return 100L * n + 25L * n * (n - 1L);
    }

    public static int levelForXp(long totalXp) {
        if (totalXp < 0) return 1;
        int level = 1;
        while (level < MAX_LEVEL && totalXp >= xpToReach(level + 1)) {
            level++;
        }
        return level;
    }

    /** XP gathered inside the current level (the carry-over). */
    public static long xpIntoLevel(long totalXp) {
        return Math.max(0L, totalXp) - xpToReach(levelForXp(totalXp));
    }

    /** Size of the current level's XP bar. */
    public static long xpNeededForNext(int level) {
        return xpToReach(level + 1) - xpToReach(level);
    }

    /** 0..100 progress through the current level. */
    public static int progressPercent(long totalXp) {
        int level = levelForXp(totalXp);
        if (level >= MAX_LEVEL) return 100;
        return (int) (xpIntoLevel(totalXp) * 100L / xpNeededForNext(level));
    }

    public static String titleForLevel(int level) {
        if (level < 3) return "Novice";
        if (level < 5) return "Apprentice";
        if (level < 10) return "Adventurer";
        if (level < 20) return "Hero";
        return "Legend";
    }
}
