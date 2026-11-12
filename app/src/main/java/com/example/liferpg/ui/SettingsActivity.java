package com.example.liferpg.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.example.liferpg.BuildConfig;
import com.example.liferpg.R;
import com.example.liferpg.data.PlayerProfile;
import com.example.liferpg.databinding.ActivitySettingsBinding;
import com.example.liferpg.repository.ProfileRepository;
import com.example.liferpg.service.MapKey;
import com.example.liferpg.util.AppPrefs;
import com.example.liferpg.util.Async;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding b;
    private ProfileRepository repo;
    private AppPrefs prefs;
    private int avatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        repo = new ProfileRepository(this);
        prefs = new AppPrefs(this);

        b.ibBack.setOnClickListener(v -> finish());

        int mode = prefs.getThemeMode();
        int checked = mode == AppCompatDelegate.MODE_NIGHT_NO ? R.id.btn_theme_light
                : mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ? R.id.btn_theme_system
                : R.id.btn_theme_dark;
        b.tgTheme.check(checked);
        b.tgTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            int newMode;
            if (checkedId == R.id.btn_theme_light) newMode = AppCompatDelegate.MODE_NIGHT_NO;
            else if (checkedId == R.id.btn_theme_system) newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            else newMode = AppCompatDelegate.MODE_NIGHT_YES;
            if (newMode != prefs.getThemeMode()) {
                prefs.setThemeMode(newMode);
                AppCompatDelegate.setDefaultNightMode(newMode);
            }
        });

        boolean keyOk = MapKey.isConfigured();
        b.tvAppInfo.setText("LIFE RPG v" + BuildConfig.VERSION_NAME
                + "\nTurn your real life into an RPG.\n"
                + "Data is stored only on this device (works offline).\n"
                + "Gym search: OpenStreetMap (no key needed)\nGoogle map view: " + (keyOk ? "enabled" : "off (optional key, see README)"));

        b.btnCustomize.setOnClickListener(v -> startActivity(new Intent(this, AvatarEditorActivity.class)));
        b.avatarView.setOnClickListener(v -> startActivity(new Intent(this, AvatarEditorActivity.class)));
        b.btnSaveProfile.setOnClickListener(v -> saveProfile());
        b.btnReset.setOnClickListener(v -> confirmReset());
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        b.avatarView.setConfig(Avatars.load(this, avatar));
    }

    private void loadProfile() {
        Async.run(() -> repo.get(), (p, err) -> {
            if (p == null) return;
            avatar = p.avatarIndex;
            b.etName.setText(p.name);
            b.avatarView.setConfig(Avatars.load(this, avatar));
        });
    }

    private void saveProfile() {
        final String name = String.valueOf(b.etName.getText()).trim();
        if (name.isEmpty()) {
            b.tilName.setError("Name cannot be empty");
            return;
        }
        b.tilName.setError(null);
        final int chosen = avatar;
        Async.run(() -> {
            repo.updateProfile(name, chosen);
            return Boolean.TRUE;
        }, (ok, err) -> {
            if (err != null) {
                b.tilName.setError(err.getMessage());
            } else {
                Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset all progress?")
                .setMessage("This permanently erases XP, level, attributes, streaks, history, achievements "
                        + "and quest completions. Your name, avatar, quests and saved gyms are kept.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", (d, w) -> Async.run(() -> {
                    repo.resetProgress();
                    return Boolean.TRUE;
                }, (ok, err) -> Toast.makeText(this,
                        err == null ? "Progress reset" : "Reset failed", Toast.LENGTH_SHORT).show()))
                .show();
    }
}
