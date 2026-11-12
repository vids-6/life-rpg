package com.example.liferpg.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.liferpg.R;
import com.example.liferpg.databinding.ActivityMainBinding;
import com.example.liferpg.util.AppPrefs;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!new AppPrefs(this).isOnboarded()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.bottomNav.setOnItemSelectedListener(item -> {
            show(item.getItemId());
            return true;
        });
        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    /** Lets fragments (dashboard shortcuts) switch tabs. */
    public void selectTab(int menuId) {
        if (binding != null) binding.bottomNav.setSelectedItemId(menuId);
    }

    private void show(int id) {
        Fragment f;
        if (id == R.id.nav_quests) f = new QuestsFragment();
        else if (id == R.id.nav_focus) f = new FocusFragment();
        else if (id == R.id.nav_stats) f = new StatsFragment();
        else if (id == R.id.nav_badges) f = new AchievementsFragment();
        else f = new DashboardFragment();
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, f)
                .commit();
    }
}
