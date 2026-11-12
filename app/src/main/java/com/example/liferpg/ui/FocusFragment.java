package com.example.liferpg.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.UUID;

import com.example.liferpg.R;
import com.example.liferpg.databinding.FragmentFocusBinding;
import com.example.liferpg.logic.FocusTimerLogic;
import com.example.liferpg.logic.XpCalculator;
import com.example.liferpg.repository.SessionRepository;
import com.example.liferpg.util.Async;
import com.example.liferpg.util.FocusStateStore;

/**
 * Focus timer. State lives in FocusTimerLogic (wall-clock based) and is persisted on every action,
 * so backgrounding, rotation or process death never loses or double-counts a session.
 * XP is granted once: the session id is the unique reward key in the database.
 */
public class FocusFragment extends Fragment {
    private FragmentFocusBinding b;
    private FocusStateStore store;
    private FocusTimerLogic logic;
    private SessionRepository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean completing;
    private boolean saveFailed;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (b == null) return;
            updateUi();
            if (logic.getState() == FocusTimerLogic.State.RUNNING) handler.postDelayed(this, 250);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentFocusBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        store = new FocusStateStore(requireContext());
        repo = new SessionRepository(requireContext());
        logic = store.load();
        Avatars.bindHeader(b.avatarMini);

        b.cgDuration.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty() || logic.getState() != FocusTimerLogic.State.IDLE) return;
            logic.selectDuration(minutesForChip(ids.get(0)));
            store.save(logic);
            updateUi();
        });
        b.btnStart.setOnClickListener(v -> onPrimaryClick());
        b.btnReset.setOnClickListener(v -> onResetClick());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (b != null) b.avatarMini.setConfig(Avatars.load(requireContext()));
        logic = store.load();
        saveFailed = false;
        selectChipForDuration(logic.getDurationMin());
        updateUi();
        handler.removeCallbacks(tick);
        if (logic.getState() == FocusTimerLogic.State.RUNNING) handler.post(tick);
        loadSummary();
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(tick);
        b = null;
    }

    private static int minutesForChip(int chipId) {
        if (chipId == R.id.chip_15) return 15;
        if (chipId == R.id.chip_45) return 45;
        if (chipId == R.id.chip_60) return 60;
        return 25;
    }

    private void selectChipForDuration(int minutes) {
        int id = minutes == 15 ? R.id.chip_15 : minutes == 45 ? R.id.chip_45
                : minutes == 60 ? R.id.chip_60 : R.id.chip_25;
        b.cgDuration.check(id);
    }

    private void onPrimaryClick() {
        long now = System.currentTimeMillis();
        switch (logic.getState()) {
            case IDLE:
                logic.start(logic.getDurationMin(), now, UUID.randomUUID().toString());
                saveFailed = false;
                break;
            case RUNNING:
                logic.pause(now);
                break;
            default:
                logic.resume(now);
                break;
        }
        store.save(logic);
        handler.removeCallbacks(tick);
        updateUi();
        if (logic.getState() == FocusTimerLogic.State.RUNNING) handler.post(tick);
    }

    private void onResetClick() {
        if (logic.getState() == FocusTimerLogic.State.IDLE) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset timer?")
                .setMessage("The session will be cancelled and no XP will be awarded.")
                .setNegativeButton("Keep going", null)
                .setPositiveButton("Reset", (d, w) -> {
                    logic.reset();
                    store.save(logic);
                    handler.removeCallbacks(tick);
                    if (b != null) updateUi();
                })
                .show();
    }

    private void updateUi() {
        if (b == null) return;
        long now = System.currentTimeMillis();
        if (logic.isFinished(now)) {
            if (!completing && !saveFailed) finishSession();
        }
        FocusTimerLogic.State state = logic.getState();
        long remaining = logic.remainingMs(now);
        b.tvTimer.setText(FocusTimerLogic.format(remaining));
        b.progressCircle.setProgressCompat((int) (logic.progress(now) * 1000), false);

        boolean idle = state == FocusTimerLogic.State.IDLE;
        for (int i = 0; i < b.cgDuration.getChildCount(); i++) b.cgDuration.getChildAt(i).setEnabled(idle);
        b.btnReset.setEnabled(!idle);
        if (idle) {
            b.tvState.setText("Ready");
            b.btnStart.setText("Start");
            b.btnStart.setIconResource(R.drawable.ic_play);
        } else if (state == FocusTimerLogic.State.RUNNING) {
            b.tvState.setText(saveFailed ? "Couldn't save yet" : completing ? "Saving your reward…" : "Focusing…");
            b.btnStart.setText("Pause");
            b.btnStart.setIconResource(R.drawable.ic_pause);
        } else {
            b.tvState.setText("Paused");
            b.btnStart.setText("Resume");
            b.btnStart.setIconResource(R.drawable.ic_play);
        }
        b.tvRewardHint.setText("Reward for " + logic.getDurationMin() + " min: +"
                + XpCalculator.focusXp(logic.getDurationMin()) + " XP");
    }

    private void finishSession() {
        completing = true;
        final String sessionId = logic.getSessionId();
        final int minutes = logic.getDurationMin();
        Async.run(() -> repo.completeFocus(sessionId, minutes), (result, err) -> {
            completing = false;
            if (err != null || result == null) {
                saveFailed = true; // retried next time the tab is shown
                if (isAdded()) Toast.makeText(requireContext(), "Couldn't save the session. Will retry.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Success OR "already claimed": either way this session is finished for good.
            logic.reset();
            store.save(logic);
            handler.removeCallbacks(tick);
            if (!isAdded() || b == null) return;
            updateUi();
            RewardUi.show(requireContext(), result);
            loadSummary();
        });
    }

    private void loadSummary() {
        Async.run(() -> repo.focusSummary(), (s, err) -> {
            if (!isAdded() || b == null || s == null) return;
            b.tvSessionsToday.setText("Sessions today: " + s[0]);
            b.tvSessionsTotal.setText("All time: " + s[1] + " sessions • " + s[2] + " focused minutes");
        });
    }
}
