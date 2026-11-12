package com.example.liferpg.model;

/** The five character attributes. Order is significant (used as array index). */
public enum Attribute {
    INTELLIGENCE("Intelligence", "🧠"),
    STRENGTH("Strength", "💪"),
    DISCIPLINE("Discipline", "🛡️"),
    CREATIVITY("Creativity", "🎨"),
    VITALITY("Vitality", "❤️");

    public final String label;
    public final String emoji;

    Attribute(String label, String emoji) {
        this.label = label;
        this.emoji = emoji;
    }
}
