package com.example.liferpg.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.liferpg.model.Attribute;

@Entity(tableName = "player_profile")
public class PlayerProfile {
    /** Single-row table: id is always 1. */
    @PrimaryKey
    public int id = 1;
    @NonNull
    public String name = "Hero";
    public int avatarIndex;
    public long totalXp;
    public int intelligence;
    public int strength;
    public int discipline;
    public int creativity;
    public int vitality;
    public int currentStreak;
    public int bestStreak;
    public String lastActiveDate;
    public long createdAt;

    public int getAttribute(Attribute a) {
        switch (a) {
            case INTELLIGENCE: return intelligence;
            case STRENGTH: return strength;
            case DISCIPLINE: return discipline;
            case CREATIVITY: return creativity;
            default: return vitality;
        }
    }

    public void addAttribute(Attribute a, int amount) {
        switch (a) {
            case INTELLIGENCE: intelligence += amount; break;
            case STRENGTH: strength += amount; break;
            case DISCIPLINE: discipline += amount; break;
            case CREATIVITY: creativity += amount; break;
            default: vitality += amount; break;
        }
    }
}
