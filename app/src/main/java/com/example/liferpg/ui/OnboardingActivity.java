package com.example.liferpg.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.liferpg.databinding.ActivityOnboardingBinding;
import com.example.liferpg.repository.ProfileRepository;
import com.example.liferpg.util.Async;

public class OnboardingActivity extends AppCompatActivity {
    private static final String KEY_STEP = "step";
    private static final String KEY_AVATAR = "avatar";

    private ActivityOnboardingBinding b;
    private int step;
    private int avatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        if (savedInstanceState != null) {
            step = savedInstanceState.getInt(KEY_STEP, 0);
            avatar = savedInstanceState.getInt(KEY_AVATAR, 0);
        }
        if (new com.example.liferpg.util.AppPrefs(this).getAvatarConfig() == null) {
            new com.example.liferpg.util.AppPrefs(this).setAvatarConfig(new AvatarConfig().serialize());
        }
        b.btnCustomize.setOnClickListener(v -> startActivity(new Intent(this, AvatarEditorActivity.class)));
        b.avatarView.setOnClickListener(v -> startActivity(new Intent(this, AvatarEditorActivity.class)));
        b.btnNext.setOnClickListener(v -> onNext());
        b.btnBack.setOnClickListener(v -> {
            step = 0;
            render();
        });
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_STEP, step);
        outState.putInt(KEY_AVATAR, avatar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        b.avatarView.setConfig(Avatars.load(this, avatar));
    }

    private void render() {
        boolean profileStep = step == 1;
        b.stepIntro.setVisibility(profileStep ? View.GONE : View.VISIBLE);
        b.stepProfile.setVisibility(profileStep ? View.VISIBLE : View.GONE);
        b.btnBack.setVisibility(profileStep ? View.VISIBLE : View.GONE);
        b.btnNext.setText(profileStep ? "Start adventure" : "Next");
        Anim.fadeInUp(profileStep ? b.stepProfile : b.stepIntro, 0);
    }

    private void onNext() {
        if (step == 0) {
            step = 1;
            render();
            return;
        }
        final String name = String.valueOf(b.etName.getText()).trim();
        if (name.isEmpty()) {
            b.tilName.setError("Every hero needs a name");
            return;
        }
        b.tilName.setError(null);
        b.btnNext.setEnabled(false);
        final int chosenAvatar = avatar;
        Async.run(() -> {
            new ProfileRepository(this).createProfile(name, chosenAvatar);
            return Boolean.TRUE;
        }, (ok, err) -> {
            if (err != null) {
                b.btnNext.setEnabled(true);
                b.tilName.setError(err.getMessage());
                return;
            }
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }
}
