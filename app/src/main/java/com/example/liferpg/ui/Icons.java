package com.example.liferpg.ui;

import android.content.res.ColorStateList;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.example.liferpg.R;
import com.example.liferpg.model.Achievement;
import com.example.liferpg.model.ActivityType;
import com.example.liferpg.model.Attribute;
import com.example.liferpg.model.Category;

/**
 * Maps game concepts to the app's single, consistent vector icon family (res/drawable/ic_*.xml).
 * The model enums keep their emoji strings (they are persisted/used by tests), but the UI never
 * shows emojis any more: every icon on screen comes from here.
 */
public final class Icons {
    private Icons() { }

    public static int category(Category c) {
        switch (c) {
            case STUDY: return R.drawable.ic_book;
            case FITNESS: return R.drawable.ic_pulse;
            case HEALTH: return R.drawable.ic_heart;
            case PERSONAL: return R.drawable.ic_sprout;
            case CREATIVITY: return R.drawable.ic_palette;
            case DISCIPLINE: return R.drawable.ic_target;
            case PROJECTS: return R.drawable.ic_scroll;
            case DAILY_LIFE: return R.drawable.ic_home;
            case CUSTOM: return R.drawable.ic_sparkle;
            default: return R.drawable.ic_star;
        }
    }

    public static int categoryColor(Category c) {
        switch (c) {
            case STUDY: return R.color.lr_attr_intelligence;
            case FITNESS: return R.color.lr_attr_strength;
            case HEALTH: return R.color.lr_attr_vitality;
            case PERSONAL: return R.color.lr_gold;
            case CREATIVITY: return R.color.lr_attr_creativity;
            case DISCIPLINE: return R.color.lr_attr_discipline;
            case PROJECTS: return R.color.lr_attr_intelligence;
            case DAILY_LIFE: return R.color.lr_attr_vitality;
            case CUSTOM: return R.color.lr_gold;
            default: return R.color.lr_primary_text;
        }
    }

    public static int attribute(Attribute a) {
        switch (a) {
            case INTELLIGENCE: return R.drawable.ic_bulb;
            case STRENGTH: return R.drawable.ic_gym;
            case DISCIPLINE: return R.drawable.ic_shield;
            case CREATIVITY: return R.drawable.ic_palette;
            case VITALITY: return R.drawable.ic_heart;
            default: return R.drawable.ic_star;
        }
    }

    public static int attributeColor(Attribute a) {
        switch (a) {
            case INTELLIGENCE: return R.color.lr_attr_intelligence;
            case STRENGTH: return R.color.lr_attr_strength;
            case DISCIPLINE: return R.color.lr_attr_discipline;
            case CREATIVITY: return R.color.lr_attr_creativity;
            case VITALITY: return R.color.lr_attr_vitality;
            default: return R.color.lr_primary_text;
        }
    }

    public static int achievement(Achievement a) {
        switch (a) {
            case FIRST_QUEST: return R.drawable.ic_sword;
            case FIRST_FOCUS: return R.drawable.ic_target;
            case TEN_QUESTS: return R.drawable.ic_scroll;
            case STREAK_5: return R.drawable.ic_fire;
            case LEVEL_5: return R.drawable.ic_crown;
            case FIRST_WORKOUT: return R.drawable.ic_gym;
            default: return R.drawable.ic_medal;
        }
    }

    public static int activity(String type) {
        if (ActivityType.QUEST.equals(type)) return R.drawable.ic_sword;
        if (ActivityType.FOCUS.equals(type)) return R.drawable.ic_timer;
        if (ActivityType.WORKOUT.equals(type)) return R.drawable.ic_gym;
        return R.drawable.ic_star;
    }

    public static int activityColor(String type) {
        if (ActivityType.QUEST.equals(type)) return R.color.lr_primary_text;
        if (ActivityType.FOCUS.equals(type)) return R.color.lr_attr_intelligence;
        if (ActivityType.WORKOUT.equals(type)) return R.color.lr_attr_vitality;
        return R.color.lr_gold_text;
    }

    /** Sets a drawable and tints it with a colour resource. */
    public static void set(ImageView view, int drawableRes, int colorRes) {
        view.setImageResource(drawableRes);
        view.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), colorRes)));
    }
}
