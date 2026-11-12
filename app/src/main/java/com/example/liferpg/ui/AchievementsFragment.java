package com.example.liferpg.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.liferpg.data.AppDatabase;
import com.example.liferpg.data.PlayerProfile;
import com.example.liferpg.data.UnlockedAchievement;
import com.example.liferpg.databinding.FragmentAchievementsBinding;
import com.example.liferpg.logic.AchievementEngine;
import com.example.liferpg.logic.LevelSystem;
import com.example.liferpg.model.Achievement;
import com.example.liferpg.model.ActivityType;
import com.example.liferpg.model.ProgressSnapshot;
import com.example.liferpg.util.Async;

public class AchievementsFragment extends Fragment {
    private FragmentAchievementsBinding b;
    private AchievementAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentAchievementsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new AchievementAdapter();
        b.rvAchievements.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        b.rvAchievements.setAdapter(adapter);
        Avatars.bindHeader(b.avatarMini);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (b != null) b.avatarMini.setConfig(Avatars.load(requireContext()));
        final android.content.Context ctx = requireContext().getApplicationContext();
        Async.run(() -> load(ctx), (items, err) -> {
            if (!isAdded() || b == null || items == null) return;
            int unlocked = 0;
            for (AchievementAdapter.Item i : items) if (i.unlocked) unlocked++;
            b.tvAchSummary.setText(unlocked + " of " + items.size() + " unlocked");
            Anim.progress(b.pbAchTotal, items.isEmpty() ? 0 : unlocked * 1000 / items.size());
            adapter.submit(items);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    /** Progress shown here is computed from the real database, never hard-coded. */
    private static List<AchievementAdapter.Item> load(android.content.Context ctx) {
        AppDatabase db = AppDatabase.get(ctx);
        PlayerProfile p = db.profileDao().get();
        ProgressSnapshot snap = new ProgressSnapshot(
                db.activityDao().count(ActivityType.QUEST),
                db.activityDao().count(ActivityType.FOCUS),
                db.activityDao().count(ActivityType.WORKOUT),
                p == null ? 0 : p.bestStreak,
                p == null ? 1 : LevelSystem.levelForXp(p.totalXp));
        Map<String, Long> unlockedAt = new HashMap<>();
        for (UnlockedAchievement u : db.achievementDao().all()) unlockedAt.put(u.key, u.unlockedAt);
        List<AchievementAdapter.Item> items = new ArrayList<>();
        for (Achievement a : Achievement.values()) {
            AchievementAdapter.Item it = new AchievementAdapter.Item();
            it.achievement = a;
            it.current = AchievementEngine.current(a, snap);
            Long at = unlockedAt.get(a.name());
            it.unlocked = at != null;
            it.unlockedAt = at == null ? 0 : at;
            items.add(it);
        }
        return items;
    }
}
