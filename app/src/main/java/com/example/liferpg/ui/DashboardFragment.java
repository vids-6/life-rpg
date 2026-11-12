package com.example.liferpg.ui;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.liferpg.R;
import com.example.liferpg.data.PlayerProfile;
import com.example.liferpg.data.Quest;
import com.example.liferpg.data.UnlockedAchievement;
import com.example.liferpg.databinding.FragmentDashboardBinding;
import com.example.liferpg.databinding.ItemActivityBinding;
import com.example.liferpg.databinding.ItemAttributeBinding;
import com.example.liferpg.databinding.ItemTodayQuestBinding;
import com.example.liferpg.logic.DateUtils;
import com.example.liferpg.logic.LevelSystem;
import com.example.liferpg.model.Achievement;
import com.example.liferpg.model.Attribute;
import com.example.liferpg.repository.ProfileRepository;
import com.example.liferpg.util.Async;

public class DashboardFragment extends Fragment {
    private static final int MAX_TODAY_ROWS = 4;

    private FragmentDashboardBinding b;
    private ProfileRepository repo;
    private boolean introAnimated;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentDashboardBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repo = new ProfileRepository(requireContext());
        b.ibSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), SettingsActivity.class)));
        View.OnClickListener editAvatar = v -> startActivity(new Intent(requireContext(), AvatarEditorActivity.class));
        b.avatarView.setOnClickListener(editAvatar);
        b.btnCustomize.setOnClickListener(editAvatar);
        View.OnClickListener openQuests = v -> ((MainActivity) requireActivity()).selectTab(R.id.nav_quests);
        b.btnQuests.setOnClickListener(openQuests);
        b.tvViewAll.setOnClickListener(openQuests);
        b.btnFocus.setOnClickListener(v -> ((MainActivity) requireActivity()).selectTab(R.id.nav_focus));
        b.btnGym.setOnClickListener(v -> startActivity(new Intent(requireContext(), GymFinderActivity.class)));
        b.btnWorkout.setOnClickListener(v -> startActivity(new Intent(requireContext(), WorkoutActivity.class)));
        // Show the saved look immediately; render() refreshes it again with the profile.
        b.avatarView.setConfig(Avatars.load(requireContext()));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (b != null) b.avatarView.setConfig(Avatars.load(requireContext()));
        Async.run(() -> repo.loadDashboard(), (data, err) -> {
            if (!isAdded() || b == null || err != null || data == null || data.profile == null) return;
            render(data);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    private void render(ProfileRepository.DashboardData data) {
        PlayerProfile p = data.profile;
        long xp = p.totalXp;
        int level = LevelSystem.levelForXp(xp);
        long into = LevelSystem.xpIntoLevel(xp);
        long need = Math.max(1, LevelSystem.xpNeededForNext(level));

        b.avatarView.setConfig(Avatars.load(requireContext(), p.avatarIndex));
        b.tvGreeting.setText(p.name);
        b.tvLevelTitle.setText(LevelSystem.titleForLevel(level));
        b.tvLevelBadge.setText(String.valueOf(level));
        b.tvLevel.setText("Level " + level);
        b.tvXpText.setText(into + " / " + need + " XP");
        Anim.progress(b.pbXp, (int) (into * 1000L / need));
        b.tvTotalXp.setText("Total " + xp + " XP  •  " + (need - into) + " to next level");

        b.tvStreak.setText(String.valueOf(data.effectiveStreak));
        b.tvBestStreak.setText((data.effectiveStreak == 1 ? "day" : "days") + " • best " + p.bestStreak);

        if (data.dailyTotal == 0) {
            b.tvDaily.setText("–");
            b.pbDaily.setProgress(0);
            b.tvDailySub.setText("Add a daily quest to track it here");
        } else {
            b.tvDaily.setText(data.dailyDone + "/" + data.dailyTotal);
            Anim.progress(b.pbDaily, data.dailyDone * 1000 / data.dailyTotal);
            b.tvDailySub.setText(data.dailyDone == data.dailyTotal
                    ? "All quests done today"
                    : (data.dailyTotal - data.dailyDone) + " left today");
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        renderTodayQuests(inflater, data);

        b.llAttributes.removeAllViews();
        int max = 10;
        for (Attribute a : Attribute.values()) max = Math.max(max, p.getAttribute(a));
        for (Attribute a : Attribute.values()) {
            ItemAttributeBinding row = ItemAttributeBinding.inflate(inflater, b.llAttributes, false);
            int value = p.getAttribute(a);
            row.tvAttrName.setText(a.label);
            row.tvAttrValue.setText(String.valueOf(value));
            Icons.set(row.ivAttr, Icons.attribute(a), Icons.attributeColor(a));
            row.pbAttr.setProgressTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), Icons.attributeColor(a))));
            b.llAttributes.addView(row.getRoot());
            Anim.progress(row.pbAttr, value * 1000 / max);
        }

        b.tvStatQuests.setText(String.valueOf(data.questsCompleted));
        b.tvStatFocus.setText(String.valueOf(data.focusSessions));
        b.tvStatWorkouts.setText(String.valueOf(data.workouts));
        b.tvAchCount.setText(data.achievementsUnlocked + " / " + data.achievementsTotal);
        Anim.progress(b.pbAch, data.achievementsTotal == 0 ? 0
                : data.achievementsUnlocked * 1000 / data.achievementsTotal);

        b.llRecentAch.removeAllViews();
        int shown = 0;
        for (UnlockedAchievement u : data.recentAchievements) {
            Achievement a = Achievement.fromKey(u.key);
            if (a == null) continue;
            ItemActivityBinding row = ItemActivityBinding.inflate(inflater, b.llRecentAch, false);
            Icons.set(row.ivIcon, Icons.achievement(a), R.color.lr_gold_text);
            row.tvTitle.setText(a.title);
            row.tvSub.setText("Unlocked " + DateUtils.formatDay(u.unlockedAt));
            row.tvXp.setVisibility(View.GONE);
            b.llRecentAch.addView(row.getRoot());
            shown++;
        }
        b.tvNoAch.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);

        if (!introAnimated) {
            introAnimated = true;
            ViewGroup root = b.rootContainer;
            for (int i = 0; i < root.getChildCount(); i++) {
                Anim.fadeInUp(root.getChildAt(i), i * 70L);
            }
        }
    }

    private void renderTodayQuests(LayoutInflater inflater, ProfileRepository.DashboardData data) {
        b.llTodayQuests.removeAllViews();
        String today = DateUtils.todayKey();
        int shown = 0;
        for (Quest q : data.todayQuests) {
            if (shown >= MAX_TODAY_ROWS) break;
            boolean done = q.isDoneToday(today);
            ItemTodayQuestBinding row = ItemTodayQuestBinding.inflate(inflater, b.llTodayQuests, false);
            row.tvTitle.setText(q.title);
            if (done) {
                Icons.set(row.ivState, R.drawable.ic_check_circle, R.color.lr_success);
                row.tvTitle.setPaintFlags(row.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                row.tvTitle.setAlpha(0.55f);
                row.tvXp.setText("Done");
                row.tvXp.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.lr_success));
            } else {
                Icons.set(row.ivState, R.drawable.ic_circle, R.color.lr_text_tertiary);
                row.tvXp.setText("+" + com.example.liferpg.logic.QuestXp.earnedXp(q.xpReward, q.recurringDaily ? 100
                        : com.example.liferpg.logic.QuestXp.latePercent(q.dueDate, System.currentTimeMillis())) + " XP");
            }
            row.getRoot().setOnClickListener(v -> ((MainActivity) requireActivity()).selectTab(R.id.nav_quests));
            b.llTodayQuests.addView(row.getRoot());
            shown++;
        }
        int hidden = data.todayQuests.size() - shown;
        b.tvTodayEmpty.setVisibility(data.todayQuests.isEmpty() ? View.VISIBLE : View.GONE);
        b.tvViewAll.setText(hidden > 0 ? "View all (+" + hidden + ")" : "View all");
    }
}
