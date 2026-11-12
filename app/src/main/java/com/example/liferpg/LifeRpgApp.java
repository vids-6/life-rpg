
package com.example.liferpg;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.liferpg.util.AppPrefs;

public class LifeRpgApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        AppCompatDelegate.setDefaultNightMode(
                new AppPrefs(this).getThemeMode()
        );
    }
}
