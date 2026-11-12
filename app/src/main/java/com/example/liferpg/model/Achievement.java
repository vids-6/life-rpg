package com.example.liferpg.model;

public enum Achievement {
    FIRST_QUEST("First Quest", "Complete your first quest", "🗡️", 1),
    FIRST_FOCUS("First Focus Session", "Finish a focus timer", "🎯", 1),
    TEN_QUESTS("10 Quests", "Complete 10 quests", "📜", 10),
    STREAK_5("5-Day Streak", "Be active 5 days in a row", "🔥", 5),
    LEVEL_5("Level 5", "Reach level 5", "👑", 5),
    FIRST_WORKOUT("First Workout", "Confirm a gym workout", "🏋️", 1);

    public final String title;
    public final String description;
    public final String emoji;
    public final int target;

    Achievement(String title, String description, String emoji, int target) {
        this.title = title;
        this.description = description;
        this.emoji = emoji;
        this.target = target;
    }

    public static Achievement fromKey(String key) {
        for (Achievement a : values()) {
            if (a.name().equals(key)) return a;
        }
        return null;
    }
}
