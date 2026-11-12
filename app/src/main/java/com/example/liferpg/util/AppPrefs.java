package com.example.liferpg.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Lightweight settings (SharedPreferences). */
public class AppPrefs {
    private static final String FILE = "liferpg_prefs";
    private static final String KEY_ONBOARDED = "onboarded";
    private static final String KEY_THEME = "theme_mode";

    private static final String KEY_AVATAR_CFG = "avatar_config";

    private final SharedPreferences sp;

    public AppPrefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public boolean isOnboarded() {
        return sp.getBoolean(KEY_ONBOARDED, false);
    }

    public void setOnboarded(boolean value) {
        sp.edit().putBoolean(KEY_ONBOARDED, value).apply();
    }

    /** One of AppCompatDelegate.MODE_NIGHT_*. Defaults to dark: the premium look is dark navy. */
    public int getThemeMode() {
        return sp.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_YES);
    }

    public void setThemeMode(int mode) {
        sp.edit().putInt(KEY_THEME, mode).apply();
    }

    /** Serialized avatar customisation; null if the player never customised. */
    public String getAvatarConfig() {
        return sp.getString(KEY_AVATAR_CFG, null);
    }

    public void setAvatarConfig(String value) {
        sp.edit().putString(KEY_AVATAR_CFG, value).apply();
    }
}
