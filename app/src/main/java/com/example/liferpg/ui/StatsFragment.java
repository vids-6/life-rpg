package com.example.liferpg.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.liferpg.R;
import com.example.liferpg.data.CategoryStat;
import com.example.liferpg.databinding.FragmentStatsBinding;
import com.example.liferpg.databinding.ItemCategoryStatBinding;
import com.example.liferpg.databinding.ItemStatTileBinding;
import com.example.liferpg.model.Category;
import com.example.liferpg.repository.StatsRepository;
import com.example.liferpg.util.Async;

public class StatsFragment extends Fragment {
    private FragmentStatsBinding b;
    private StatsRepository repo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentStatsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repo = new StatsRepository(requireContext());
        Avatars.bindHeader(b.avatarMini);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (b != null) b.avatarMini.setConfig(Avatars.load(requireContext()));
        Async.run(() -> repo.load(), (s, err) -> {
            if (!isAdded() || b == null || s == null) return;
            render(s);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    private void addTile(LayoutInflater inflater, int icon, int color, String value, String label) {
        ItemStatTileBinding t = ItemStatTileBinding.inflate(inflater, b.gridTiles, false);
        Icons.set(t.ivIcon, icon, color);
        t.tvValue.setText(value);
        t.tvLabel.setText(label);
        float d = getResources().getDisplayMetrics().density;
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        lp.setMargins((int) (4 * d), (int) (4 * d), (int) (4 * d), (int) (4 * d));
        t.getRoot().setLayoutParams(lp);
        b.gridTiles.addView(t.getRoot());
    }

    private void render(StatsRepository.StatsData s) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        b.gridTiles.removeAllViews();
        addTile(inflater, R.drawable.ic_crown, R.color.lr_gold_text, String.valueOf(s.level), "Current level");
        addTile(inflater, R.drawable.ic_bolt, R.color.lr_gold_text, String.valueOf(s.totalXp), "Total XP");
        addTile(inflater, R.drawable.ic_sword, R.color.lr_primary_text, String.valueOf(s.questCount), "Quests completed");
        addTile(inflater, R.drawable.ic_gym, R.color.lr_attr_vitality, String.valueOf(s.workoutCount),
                "Workouts (" + s.workoutMinutes + " min)");
        addTile(inflater, R.drawable.ic_timer, R.color.lr_attr_intelligence, String.valueOf(s.focusCount),
                "Focus sessions (" + s.focusMinutes + " min)");
        addTile(inflater, R.drawable.ic_fire, R.color.lr_gold_text, s.currentStreak + " / " + s.bestStreak,
                "Streak (current / best)");
        addTile(inflater, R.drawable.ic_sparkle, R.color.lr_attr_creativity, "+" + s.todayXp, "XP today");
        addTile(inflater, R.drawable.ic_calendar, R.color.lr_attr_discipline, "+" + s.weekXp, "XP this week");

        b.chartWeekly.setData(s.dayLabels, s.dayXp);

        b.llCategories.removeAllViews();
        int max = 1;
        for (CategoryStat c : s.categories) max = Math.max(max, c.count);
        for (CategoryStat c : s.categories) {
            Category cat = Category.fromName(c.category);
            ItemCategoryStatBinding row = ItemCategoryStatBinding.inflate(inflater, b.llCategories, false);
            row.tvCatName.setText(cat.label);
            Icons.set(row.ivCat, Icons.category(cat), Icons.categoryColor(cat));
            row.pbCat.setProgressTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(requireContext(), Icons.categoryColor(cat))));
            row.tvCatInfo.setText(c.count + " activities • " + c.xp + " XP");
            b.llCategories.addView(row.getRoot());
            Anim.progress(row.pbCat, c.count * 1000 / max);
        }
        b.tvNoCategories.setVisibility(s.categories.isEmpty() ? View.VISIBLE : View.GONE);

        ActivityAdapter.fill(inflater, b.llHistory, s.recent);
        b.tvNoHistory.setVisibility(s.recent.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
