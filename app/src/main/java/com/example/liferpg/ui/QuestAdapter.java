package com.example.liferpg.ui;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.example.liferpg.R;
import com.example.liferpg.data.Quest;
import com.example.liferpg.databinding.ItemQuestBinding;
import com.example.liferpg.logic.DateUtils;
import com.example.liferpg.model.Category;
import com.example.liferpg.logic.QuestXp;
import com.example.liferpg.model.Difficulty;

public class QuestAdapter extends RecyclerView.Adapter<QuestAdapter.Holder> {

    public interface Listener {
        void onComplete(Quest quest);

        void onEdit(Quest quest);

        void onDelete(Quest quest);
    }

    private final List<Quest> items = new ArrayList<>();
    private final Listener listener;
    private String today = DateUtils.todayKey();

    public QuestAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Quest> quests) {
        today = DateUtils.todayKey();
        items.clear();
        items.addAll(quests);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemQuestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final Quest q = items.get(position);
        Context ctx = h.b.getRoot().getContext();
        Category cat = Category.fromName(q.category);
        Difficulty diff = Difficulty.fromName(q.difficulty);
        boolean done = q.isDoneToday(today);

        Icons.set(h.b.ivIcon, Icons.category(cat), Icons.categoryColor(cat));
        h.b.tvTitle.setText(q.title);
        h.b.tvMeta.setText(cat.label + " • " + diff.label
                + (q.durationMin > 0 ? " • " + QuestXp.formatDuration(q.durationMin) : "")
                + (q.recurringDaily ? " • Daily" : ""));
        // What completing it right now would pay (late quests pay a share of the base XP).
        int percent = (done || q.recurringDaily) ? 100 : QuestXp.latePercent(q.dueDate, System.currentTimeMillis());
        h.b.tvXp.setText("+" + QuestXp.earnedXp(q.xpReward, percent) + " XP");
        h.b.tvXp.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_bolt, 0, 0, 0);
        h.b.tvXp.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(ctx, R.color.lr_gold_text)));
        h.b.tvTitle.setPaintFlags(done
                ? h.b.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : h.b.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

        if (q.dueDate > 0 && !q.recurringDaily) {
            boolean overdue = !done && DateUtils.keyForMillis(q.dueDate).compareTo(today) < 0;
            h.b.tvDue.setVisibility(android.view.View.VISIBLE);
            h.b.tvDue.setText((overdue ? "Overdue • due " : "Due ") + DateUtils.formatDay(q.dueDate)
                    + (percent < 100 ? " • " + percent + "% XP (base " + q.xpReward + ")" : ""));
            h.b.tvDue.setTextColor(ContextCompat.getColor(ctx, overdue ? R.color.lr_danger : R.color.lr_text_secondary));
        } else {
            h.b.tvDue.setVisibility(android.view.View.GONE);
        }

        if (done) {
            h.b.btnComplete.setEnabled(false);
            h.b.btnComplete.setText(q.recurringDaily ? "Done today" : "Completed");
            h.b.tvStatus.setText(q.recurringDaily
                    ? "Resets tomorrow • done " + q.timesCompleted + "x total"
                    : "Completed " + DateUtils.formatDay(q.completedAt));
            h.b.getRoot().setAlpha(0.65f);
        } else {
            h.b.btnComplete.setEnabled(true);
            h.b.btnComplete.setText("Complete");
            h.b.tvStatus.setText(q.timesCompleted > 0 ? "Completed " + q.timesCompleted + "x before" : "");
            h.b.getRoot().setAlpha(1f);
        }

        h.b.btnComplete.setOnClickListener(v -> {
            v.setEnabled(false); // block double taps; the DB also guards against duplicates
            Anim.pop(h.b.ivIcon);
            Anim.completeFlash(h.b.getRoot());
            listener.onComplete(q);
        });
        h.b.ibEdit.setOnClickListener(v -> listener.onEdit(q));
        h.b.ibDelete.setOnClickListener(v -> listener.onDelete(q));
        h.b.getRoot().setOnClickListener(v -> listener.onEdit(q));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemQuestBinding b;

        Holder(ItemQuestBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
