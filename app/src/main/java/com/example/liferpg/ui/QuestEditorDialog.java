package com.example.liferpg.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.example.liferpg.R;
import com.example.liferpg.data.ActivityLog;
import com.example.liferpg.data.Quest;
import com.example.liferpg.databinding.DialogQuestBinding;
import com.example.liferpg.logic.DateUtils;
import com.example.liferpg.logic.QuestXp;
import com.example.liferpg.model.Category;
import com.example.liferpg.model.Difficulty;

/** Create / edit dialog for quests with inline validation. */
public final class QuestEditorDialog {

    public interface OnSave {
        void onSave(Quest quest);
    }

    private QuestEditorDialog() { }

    public static void show(final Context ctx, final Quest existing, List<ActivityLog> history, final OnSave callback) {
        final DialogQuestBinding d = DialogQuestBinding.inflate(LayoutInflater.from(ctx));
        final Quest q = existing != null ? existing.copy() : new Quest();
        final boolean rewardLocked = existing != null && existing.isRewardLocked();
        final boolean repeatLocked = existing != null && existing.isRepeatLocked();

        // Current categories, plus the quest's own legacy category when editing an older quest.
        final List<Category> cats = new ArrayList<>(Arrays.asList(Category.SELECTABLE));
        Category current = Category.fromName(q.category);
        if (existing != null && !cats.contains(current)) cats.add(current);
        if (existing == null) current = Category.STUDY;
        final Category[] category = {current};
        final Difficulty[] difficulty = {Difficulty.fromName(q.difficulty)};
        final int[] duration = {q.durationMin};
        // New quests start with a sensible duration selected; legacy quests keep their stored XP until one is chosen.
        if (existing == null) duration[0] = QuestXp.durationOptions(category[0])[1];

        String[] catLabels = new String[cats.size()];
        for (int i = 0; i < catLabels.length; i++) catLabels[i] = cats.get(i).label;
        String[] diffLabels = new String[Difficulty.values().length];
        for (int i = 0; i < diffLabels.length; i++) diffLabels[i] = Difficulty.values()[i].label;
        d.actCategory.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_1, catLabels));
        d.actDifficulty.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_1, diffLabels));

        d.etTitle.setText(q.title);
        d.etDescription.setText(q.description);
        d.actCategory.setText(category[0].label, false);
        d.actDifficulty.setText(difficulty[0].label, false);
        d.swDaily.setChecked(q.recurringDaily);

        final long[] due = {q.dueDate};
        // A due date the user chose (or that an existing quest already has) is never overwritten by the suggestion.
        final boolean[] dueTouched = {existing != null};
        if (existing == null) due[0] = QuestXp.suggestedDueDate(duration[0]);

        final Runnable refreshPreview = () -> {
            if (duration[0] > 0) {
                int base = QuestXp.baseXp(difficulty[0], duration[0]);
                d.tvXpPreview.setText("Reward: " + base + " XP\n" + difficulty[0].label + " "
                        + difficulty[0].defaultXp + " × "
                        + String.format(Locale.US, "%.2f", QuestXp.durationMultiplierPercent(duration[0]) / 100.0)
                        + " (" + QuestXp.formatDuration(duration[0]) + ")");
            } else {
                d.tvXpPreview.setText("Reward: " + q.xpReward + " XP (set before durations existed)\n"
                        + "Pick a duration to calculate it from difficulty and time.");
            }
        };
        final Runnable refreshDue = () -> {
            boolean daily = d.swDaily.isChecked();
            d.btnDue.setEnabled(!daily && !rewardLocked);
            d.btnClearDue.setEnabled(!daily && !rewardLocked && due[0] > 0);
            d.btnDue.setText(daily ? "Daily quests have no due date"
                    : due[0] > 0 ? "Due: " + DateUtils.formatDay(due[0]) : "Set due date");
            if (daily) {
                d.tvDueHint.setText("Daily quests reset every day and are never late.");
            } else if (due[0] > 0) {
                d.tvDueHint.setText((dueTouched[0] ? "" : "Suggested from the duration; tap to change. ")
                        + "Finishing late pays less: up to 24h late 80%, 48h 60%, 72h 40%, after that 25%.");
            } else {
                d.tvDueHint.setText("No due date: the quest never counts as late.");
            }
        };
        final Runnable rebuildChips = () -> {
            d.cgDuration.removeAllViews();
            for (int m : QuestXp.durationOptions(category[0])) {
                Chip chip = (Chip) LayoutInflater.from(ctx).inflate(R.layout.item_chip, d.cgDuration, false);
                chip.setId(View.generateViewId());
                chip.setText(QuestXp.formatDuration(m));
                chip.setTag(m);
                chip.setEnabled(!rewardLocked);
                chip.setChecked(m == duration[0]);
                d.cgDuration.addView(chip);
            }
        };
        rebuildChips.run();
        refreshPreview.run();
        refreshDue.run();

        d.cgDuration.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            Chip c = group.findViewById(ids.get(0));
            duration[0] = (Integer) c.getTag();
            if (!dueTouched[0]) due[0] = QuestXp.suggestedDueDate(duration[0]);
            refreshPreview.run();
            refreshDue.run();
        });
        d.actCategory.setOnItemClickListener((parent, view, position, id) -> {
            category[0] = cats.get(position);
            int[] opts = QuestXp.durationOptions(category[0]);
            boolean valid = false;
            for (int m : opts) if (m == duration[0]) valid = true;
            if (!valid) duration[0] = opts[1];
            rebuildChips.run();
            if (!dueTouched[0]) due[0] = QuestXp.suggestedDueDate(duration[0]);
            refreshPreview.run();
            refreshDue.run();
        });
        d.actDifficulty.setOnItemClickListener((parent, view, position, id) -> {
            difficulty[0] = Difficulty.values()[position];
            refreshPreview.run();
        });
        d.swDaily.setOnCheckedChangeListener((b, checked) -> refreshDue.run());
        d.btnClearDue.setOnClickListener(v -> {
            due[0] = 0;
            dueTouched[0] = true;
            refreshDue.run();
        });
        d.btnDue.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            if (due[0] > 0) c.setTimeInMillis(due[0]);
            new DatePickerDialog(ctx, (view, y, m, day) -> {
                due[0] = DateUtils.startOfDayMillis(y, m + 1, day);
                dueTouched[0] = true;
                refreshDue.run();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        if (rewardLocked) {
            d.actCategory.setEnabled(false);
            d.actDifficulty.setEnabled(false);
            d.swDaily.setEnabled(false);
            d.tvLockNote.setVisibility(View.VISIBLE);
            d.tvLockNote.setText("This quest is completed, so its reward settings are locked. "
                    + "You can still change the title and description.");
        } else if (repeatLocked) {
            d.swDaily.setEnabled(false);
            d.tvLockNote.setVisibility(View.VISIBLE);
            d.tvLockNote.setText("This quest has already paid out, so it can't switch between one-off and daily.");
        }

        if (existing == null || history == null || history.isEmpty()) {
            d.tvHistory.setText(existing == null ? "" : "Completion history: none yet.");
        } else {
            StringBuilder sb = new StringBuilder("Completion history (latest first):");
            for (ActivityLog log : history) {
                sb.append("\n• ").append(DateUtils.formatDateTime(log.timestamp)).append("  +").append(log.xp).append(" XP");
            }
            d.tvHistory.setText(sb.toString());
        }

        final AlertDialog dialog = new MaterialAlertDialogBuilder(ctx)
                .setTitle(existing == null ? "New quest" : "Edit quest")
                .setView(d.getRoot())
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = String.valueOf(d.etTitle.getText()).trim();
            if (title.isEmpty()) {
                d.tilTitle.setError("Give your quest a title");
                return;
            }
            d.tilTitle.setError(null);
            q.title = title;
            q.description = String.valueOf(d.etDescription.getText()).trim();
            q.category = category[0].name();
            q.difficulty = difficulty[0].name();
            q.durationMin = duration[0];
            if (duration[0] > 0) q.xpReward = QuestXp.baseXp(difficulty[0], duration[0]);
            q.recurringDaily = d.swDaily.isChecked();
            q.dueDate = q.recurringDaily ? 0 : due[0];
            dialog.dismiss();
            callback.onSave(q);
        });
    }
}
