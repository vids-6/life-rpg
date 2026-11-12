package com.example.liferpg.model;

/** String constants stored in the activity log. */
public final class ActivityType {
    public static final String QUEST = "QUEST";
    public static final String FOCUS = "FOCUS";
    public static final String WORKOUT = "WORKOUT";

    private ActivityType() { }

    public static String emoji(String type) {
        if (QUEST.equals(type)) return "⚔️";
        if (FOCUS.equals(type)) return "⏱️";
        if (WORKOUT.equals(type)) return "🏋️";
        return "⭐";
    }
}
