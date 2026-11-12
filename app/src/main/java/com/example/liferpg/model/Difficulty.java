package com.example.liferpg.model;

public enum Difficulty {
    EASY("Easy", 20),
    MEDIUM("Medium", 40),
    HARD("Hard", 70),
    EPIC("Epic", 120);

    public final String label;
    /** Base XP before the duration multiplier. */
    public final int defaultXp;

    Difficulty(String label, int defaultXp) {
        this.label = label;
        this.defaultXp = defaultXp;
    }

    public static Difficulty fromName(String name) {
        if (name != null) {
            for (Difficulty d : values()) {
                if (d.name().equals(name)) return d;
            }
        }
        return MEDIUM;
    }

    public static Difficulty fromLabel(String label) {
        for (Difficulty d : values()) {
            if (d.label.equals(label)) return d;
        }
        return MEDIUM;
    }
}
