package com.example.liferpg.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.example.liferpg.R;
import com.example.liferpg.databinding.ItemAchievementBinding;
import com.example.liferpg.logic.DateUtils;
import com.example.liferpg.model.Achievement;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.Holder> {

    /** One badge with its real progress. */
    public static class Item {
        public Achievement achievement;
        public boolean unlocked;
        public long unlockedAt;
        public int current;
    }

    private final List<Item> items = new ArrayList<>();

    public void submit(List<Item> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemAchievementBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Item it = items.get(position);
        Achievement a = it.achievement;
        Icons.set(h.b.ivBadge, Icons.achievement(a),
                it.unlocked ? R.color.lr_on_gold : R.color.lr_text_tertiary);
        h.b.tvTitle.setText(a.title);
        h.b.tvDesc.setText(a.description);
        int shown = Math.min(it.current, a.target);
        Anim.progress(h.b.pbProgress, shown * 1000 / a.target);
        if (it.unlocked) {
            h.b.ivBadge.setBackgroundResource(R.drawable.bg_badge_unlocked);
            h.b.ivBadge.setAlpha(1f);
            h.b.ivLock.setVisibility(View.GONE);
            h.b.tvState.setText("Unlocked " + DateUtils.formatDay(it.unlockedAt));
            h.b.getRoot().setAlpha(1f);
        } else {
            h.b.ivBadge.setBackgroundResource(R.drawable.bg_badge_locked);
            h.b.ivBadge.setAlpha(0.7f);
            h.b.ivLock.setVisibility(View.VISIBLE);
            h.b.tvState.setText("Locked • " + shown + " / " + a.target);
            h.b.getRoot().setAlpha(0.9f);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemAchievementBinding b;

        Holder(ItemAchievementBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
