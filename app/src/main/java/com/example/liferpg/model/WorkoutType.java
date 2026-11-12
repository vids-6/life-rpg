package com.example.liferpg.model;

public enum WorkoutType {
    STRENGTH("Strength Training", 1.2f),
    CARDIO("Cardio", 1.1f),
    GENERAL("General Fitness", 1.0f);

    public final String label;
    public final float multiplier;

    WorkoutType(String label, float multiplier) {
        this.label = label;
        this.multiplier = multiplier;
    }

    public static WorkoutType fromName(String name) {
        if (name != null) {
            for (WorkoutType t : values()) {
                if (t.name().equals(name)) return t;
            }
        }
        return GENERAL;
    }
}
