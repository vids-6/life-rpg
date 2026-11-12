package com.example.liferpg.model;

/** Quest categories and the attributes they train (primary gets full gain, secondary half). */
public enum Category {
    STUDY("Study", "📚", Attribute.INTELLIGENCE, Attribute.DISCIPLINE),
    FITNESS("Fitness", "🏃", Attribute.STRENGTH, Attribute.VITALITY),
    HEALTH("Health", "❤️", Attribute.VITALITY, Attribute.DISCIPLINE),
    PERSONAL("Personal Growth", "🌱", Attribute.DISCIPLINE, Attribute.INTELLIGENCE),
    CREATIVITY("Creativity", "🎨", Attribute.CREATIVITY, Attribute.INTELLIGENCE),
    DISCIPLINE("Discipline", "🧘", Attribute.DISCIPLINE, Attribute.VITALITY),
    // Added with the duration-based XP system.
    PROJECTS("Projects", "🛠️", Attribute.DISCIPLINE, Attribute.CREATIVITY),
    DAILY_LIFE("Daily Life", "🏠", Attribute.VITALITY, Attribute.DISCIPLINE),
    CUSTOM("Custom", "✨", Attribute.DISCIPLINE, Attribute.INTELLIGENCE);

    /**
     * Categories offered when creating a quest. HEALTH, CREATIVITY and DISCIPLINE are legacy values: quests
     * saved earlier keep them (nothing is migrated or renamed), but new quests use the six below.
     */
    public static final Category[] SELECTABLE = {STUDY, FITNESS, PERSONAL, PROJECTS, DAILY_LIFE, CUSTOM};

    public boolean isLegacy() {
        return this == HEALTH || this == CREATIVITY || this == DISCIPLINE;
    }

    public final String label;
    public final String emoji;
    public final Attribute primary;
    public final Attribute secondary;

    Category(String label, String emoji, Attribute primary, Attribute secondary) {
        this.label = label;
        this.emoji = emoji;
        this.primary = primary;
        this.secondary = secondary;
    }

    public static Category fromName(String name) {
        if (name != null) {
            for (Category c : values()) {
                if (c.name().equals(name)) return c;
            }
        }
        return PERSONAL;
    }

    public static Category fromLabel(String label) {
        for (Category c : values()) {
            if (c.label.equals(label)) return c;
        }
        return PERSONAL;
    }
}
