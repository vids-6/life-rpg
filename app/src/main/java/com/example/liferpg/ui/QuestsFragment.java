package com.example.liferpg.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import com.example.liferpg.R;
import com.example.liferpg.data.ActivityLog;
import com.example.liferpg.data.Quest;
import com.example.liferpg.databinding.FragmentQuestsBinding;
import com.example.liferpg.logic.DateUtils;
import com.example.liferpg.model.Category;
import com.example.liferpg.repository.QuestRepository;
import com.example.liferpg.util.Async;

public class QuestsFragment extends Fragment implements QuestAdapter.Listener {
    private static final int STATUS_ALL = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_DONE = 2;
    private static final int STATUS_DAILY = 3;

    private FragmentQuestsBinding b;
    private QuestRepository repo;
    private QuestAdapter adapter;
    private List<Quest> all = new ArrayList<>();
    private Category categoryFilter;
    private int statusFilter = STATUS_ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentQuestsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repo = new QuestRepository(requireContext());
        adapter = new QuestAdapter(this);
        b.rvQuests.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvQuests.setAdapter(adapter);

        Avatars.bindHeader(b.avatarMini);
        addChip(b.cgCategory, "All", null, true, 0);
        for (Category c : Category.SELECTABLE) addChip(b.cgCategory, c.label, c, false, Icons.category(c));
        String[] statusLabels = {"All", "Active", "Completed", "Daily"};
        for (int i = 0; i < statusLabels.length; i++) addChip(b.cgStatus, statusLabels[i], i, i == 0, 0);

        b.cgCategory.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            Chip c = group.findViewById(ids.get(0));
            categoryFilter = (Category) c.getTag();
            applyFilter();
        });
        b.cgStatus.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            Chip c = group.findViewById(ids.get(0));
            statusFilter = (Integer) c.getTag();
            applyFilter();
        });
        b.fabAdd.setOnClickListener(v -> openEditor(null));
    }

    private void addChip(ChipGroup group, String text, Object tag, boolean checked, int iconRes) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip, group, false);
        chip.setId(View.generateViewId());
        chip.setText(text);
        if (iconRes != 0) {
            chip.setChipIconResource(iconRes);
            chip.setChipIconVisible(true);
        }
        chip.setTag(tag);
        chip.setChecked(checked);
        group.addView(chip);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (b != null) b.avatarMini.setConfig(Avatars.load(requireContext()));
        reload();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    private void reload() {
        Async.run(() -> repo.getAll(), (list, err) -> {
            if (!isAdded() || b == null) return;
            if (err != null || list == null) {
                Toast.makeText(requireContext(), "Could not load quests", Toast.LENGTH_SHORT).show();
                return;
            }
            all = list;
            addLegacyCategoryChips();
            applyFilter();
        });
    }

    /** Quests saved before the new categories keep theirs; give them a filter chip only while they exist. */
    private void addLegacyCategoryChips() {
        for (Category c : Category.values()) {
            if (!c.isLegacy()) continue;
            boolean used = false;
            for (Quest q : all) if (c.name().equals(q.category)) used = true;
            boolean has = false;
            for (int i = 0; i < b.cgCategory.getChildCount(); i++) {
                if (c == b.cgCategory.getChildAt(i).getTag()) has = true;
            }
            if (used && !has) addChip(b.cgCategory, c.label, c, false, Icons.category(c));
        }
    }

    private void applyFilter() {
        if (b == null) return;
        String today = DateUtils.todayKey();
        List<Quest> shown = new ArrayList<>();
        int done = 0;
        for (Quest q : all) {
            boolean isDone = q.isDoneToday(today);
            if (isDone) done++;
            if (categoryFilter != null && !categoryFilter.name().equals(q.category)) continue;
            if (statusFilter == STATUS_ACTIVE && isDone) continue;
            if (statusFilter == STATUS_DONE && !isDone) continue;
            if (statusFilter == STATUS_DAILY && !q.recurringDaily) continue;
            shown.add(q);
        }
        adapter.submit(shown);
        b.tvSummary.setText(all.size() + " quests • " + done + " completed");
        b.tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        b.ivEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        b.tvEmpty.setText(all.isEmpty()
                ? "No quests yet.\nTap + to create your first quest!"
                : "No quests match these filters.");
    }

    private void openEditor(final Quest existing) {
        if (existing == null) {
            showEditor(null, null);
            return;
        }
        Async.run(() -> repo.history(existing.id), (history, err) -> {
            if (!isAdded()) return;
            showEditor(existing, history);
        });
    }

    private void showEditor(Quest existing, List<com.example.liferpg.data.ActivityLog> history) {
        QuestEditorDialog.show(requireContext(), existing, history, quest ->
                Async.run(() -> {
                    repo.save(quest);
                    return Boolean.TRUE;
                }, (ok, err) -> {
                    if (!isAdded()) return;
                    if (err != null) {
                        Toast.makeText(requireContext(), err.getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        reload();
                    }
                }));
    }

    @Override
    public void onComplete(Quest quest) {
        final long id = quest.id;
        Async.run(() -> repo.complete(id), (result, err) -> {
            if (!isAdded()) return;
            if (err != null) {
                Toast.makeText(requireContext(), "Could not complete quest", Toast.LENGTH_SHORT).show();
            } else {
                RewardUi.show(requireContext(), result);
            }
            reload();
        });
    }

    @Override
    public void onEdit(Quest quest) {
        openEditor(quest);
    }

    @Override
    public void onDelete(final Quest quest) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete quest?")
                .setMessage("\"" + quest.title + "\" will be removed. XP you already earned is kept.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> Async.run(() -> {
                    repo.delete(quest.id);
                    return Boolean.TRUE;
                }, (ok, err) -> {
                    if (isAdded()) reload();
                }))
                .show();
    }
}
