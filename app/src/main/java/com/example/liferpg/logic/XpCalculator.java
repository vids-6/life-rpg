package com.example.liferpg.logic;

import com.example.liferpg.model.Attribute;
import com.example.liferpg.model.Category;
import com.example.liferpg.model.WorkoutType;

/** All reward maths lives here so it can be unit tested without Android. */
public final class XpCalculator {
    public static final int MIN_QUEST_XP = 1;
    public static final int MAX_QUEST_XP = 500;
    public static final int MIN_WORKOUT_MINUTES = 5;
    public static final int MAX_WORKOUT_MINUTES = 120;

    private XpCalculator() { }

    public static int sanitizeQuestXp(int xp) {
        return Math.max(MIN_QUEST_XP, Math.min(MAX_QUEST_XP, xp));
    }

    public static boolean isValidQuestXp(int xp) {
        return xp >= MIN_QUEST_XP && xp <= MAX_QUEST_XP;
    }

    public static int focusXp(int minutes) {
        switch (minutes) {
            case 15: return 20;
            case 25: return 35;
            case 45: return 70;
            case 60: return 100;
            default: return Math.max(0, Math.round(minutes * 1.5f));
        }
    }

    /** 0 when the workout is shorter than the minimum. */
    public static int workoutXp(WorkoutType type, int minutes) {
        if (minutes < MIN_WORKOUT_MINUTES) return 0;
        int counted = Math.min(minutes, MAX_WORKOUT_MINUTES);
        return Math.round((20 + counted) * type.multiplier);
    }

    private static int primaryPoints(int xp) {
        return Math.max(1, xp / 20);
    }

    public static AttributeGain questGain(Category category, int xp) {
        int p = primaryPoints(xp);
        AttributeGain g = new AttributeGain().add(category.primary, p);
        if (p / 2 > 0) g.add(category.secondary, p / 2);
        return g;
    }

    public static AttributeGain focusGain(int xp) {
        int p = primaryPoints(xp);
        AttributeGain g = new AttributeGain().add(Attribute.INTELLIGENCE, p);
        if (p / 2 > 0) g.add(Attribute.DISCIPLINE, p / 2);
        return g;
    }

    public static AttributeGain workoutGain(WorkoutType type, int xp) {
        int p = primaryPoints(xp);
        int half = Math.max(1, p / 2);
        AttributeGain g = new AttributeGain();
        switch (type) {
            case STRENGTH:
                g.add(Attribute.STRENGTH, p).add(Attribute.VITALITY, half);
                break;
            case CARDIO:
                g.add(Attribute.VITALITY, p).add(Attribute.STRENGTH, half);
                break;
            default:
                g.add(Attribute.STRENGTH, half).add(Attribute.VITALITY, half);
                break;
        }
        return g;
    }
}
