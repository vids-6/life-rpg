package com.example.liferpg.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.example.liferpg.R;
import com.example.liferpg.data.SavedGym;
import com.example.liferpg.databinding.ActivityWorkoutBinding;
import com.example.liferpg.logic.XpCalculator;
import com.example.liferpg.model.WorkoutType;
import com.example.liferpg.repository.GymRepository;
import com.example.liferpg.repository.SessionRepository;
import com.example.liferpg.util.Async;
import com.example.liferpg.util.WorkoutSessionStore;

/**
 * Workout flow: pick gym -> pick type -> Start -> Finish -> confirm. XP is granted ONLY on the
 * final confirmation (and only once per session id). Merely finding or opening a gym never rewards XP.
 */
public class WorkoutActivity extends AppCompatActivity {
    public static final String EXTRA_GYM_NAME = "gym_name";
    public static final String EXTRA_GYM_ID = "gym_id";

    private static final String OTHER = "Other (type a gym name)";

    private ActivityWorkoutBinding b;
    private WorkoutSessionStore store;
    private SessionRepository sessions;
    private WorkoutSessionStore.Session active;
    private final List<String> gymNames = new ArrayList<>();
    private final List<String> gymIds = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean saving;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateElapsed();
            if (active != null) handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityWorkoutBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        store = new WorkoutSessionStore(this);
        sessions = new SessionRepository(this);

        b.ibBack.setOnClickListener(v -> finish());
        b.btnStartWorkout.setOnClickListener(v -> startWorkout());
        b.btnFinishWorkout.setOnClickListener(v -> confirmFinish());
        b.btnCancelWorkout.setOnClickListener(v -> confirmCancel());
        b.actGym.setOnItemClickListener((parent, view, position, id) -> updateCustomVisibility());
        b.cgType.setOnCheckedStateChangeListener((g, ids) -> updateXpPreview());

        b.tvWorkoutHint.setText("Earn XP only by completing and confirming a workout of at least "
                + XpCalculator.MIN_WORKOUT_MINUTES + " minutes (XP counts up to "
                + XpCalculator.MAX_WORKOUT_MINUTES + " min). Strength Training boosts Strength, Cardio boosts Vitality, "
                + "General Fitness trains both. Finding a gym alone gives no XP.");

        active = store.load();
        loadGyms();
    }

    @Override
    protected void onResume() {
        super.onResume();
        active = store.load();
        renderState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void loadGyms() {
        Async.run(() -> new GymRepository(this).getSaved(), (saved, err) -> {
            gymNames.clear();
            gymIds.clear();
            String preName = getIntent().getStringExtra(EXTRA_GYM_NAME);
            String preId = getIntent().getStringExtra(EXTRA_GYM_ID);
            int selected = -1;
            if (preName != null && !preName.isEmpty()) {
                gymNames.add(preName);
                gymIds.add(preId == null ? "" : preId);
                selected = 0;
            }
            if (saved != null) {
                for (SavedGym g : saved) {
                    if (g.placeId.equals(preId)) continue;
                    gymNames.add(g.name);
                    gymIds.add(g.placeId);
                }
            }
            List<String> display = new ArrayList<>(gymNames);
            display.add(OTHER);
            b.actGym.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, display));
            if (active != null) {
                b.actGym.setText(active.gymName, false);
            } else if (selected >= 0) {
                b.actGym.setText(gymNames.get(selected), false);
            } else if (!gymNames.isEmpty()) {
                b.actGym.setText(gymNames.get(0), false);
            } else {
                b.actGym.setText(OTHER, false);
            }
            updateCustomVisibility();
            renderState();
        });
    }

    private boolean isOtherSelected() {
        return OTHER.contentEquals(b.actGym.getText());
    }

    private void updateCustomVisibility() {
        b.tilCustomGym.setVisibility(isOtherSelected() && active == null ? View.VISIBLE : View.GONE);
    }

    private WorkoutType selectedType() {
        int id = b.cgType.getCheckedChipId();
        if (id == R.id.chip_strength) return WorkoutType.STRENGTH;
        if (id == R.id.chip_cardio) return WorkoutType.CARDIO;
        return WorkoutType.GENERAL;
    }

    private void selectType(WorkoutType t) {
        b.cgType.check(t == WorkoutType.STRENGTH ? R.id.chip_strength
                : t == WorkoutType.CARDIO ? R.id.chip_cardio : R.id.chip_general);
    }

    private void startWorkout() {
        String gymName;
        String gymId = "";
        if (isOtherSelected()) {
            gymName = String.valueOf(b.etCustomGym.getText()).trim();
            if (gymName.isEmpty()) {
                b.tilCustomGym.setError("Enter the gym you are training at");
                return;
            }
        } else {
            gymName = String.valueOf(b.actGym.getText()).trim();
            int idx = gymNames.indexOf(gymName);
            if (idx >= 0) gymId = gymIds.get(idx);
        }
        if (gymName.isEmpty()) {
            Toast.makeText(this, "Select a gym first", Toast.LENGTH_SHORT).show();
            return;
        }
        b.tilCustomGym.setError(null);
        WorkoutSessionStore.Session s = new WorkoutSessionStore.Session();
        s.sessionId = UUID.randomUUID().toString();
        s.startMs = System.currentTimeMillis();
        s.gymName = gymName;
        s.gymId = gymId;
        s.type = selectedType();
        store.save(s);
        active = s;
        renderState();
    }

    private void renderState() {
        if (b == null) return;
        boolean running = active != null;
        b.btnStartWorkout.setVisibility(running ? View.GONE : View.VISIBLE);
        b.btnFinishWorkout.setVisibility(running ? View.VISIBLE : View.GONE);
        b.btnCancelWorkout.setVisibility(running ? View.VISIBLE : View.GONE);
        b.actGym.setEnabled(!running);
        for (int i = 0; i < b.cgType.getChildCount(); i++) b.cgType.getChildAt(i).setEnabled(!running);
        handler.removeCallbacks(tick);
        if (running) {
            b.actGym.setText(active.gymName, false);
            selectType(active.type);
            b.tvWorkoutInfo.setText(active.gymName + " • " + active.type.label);
            handler.post(tick);
        } else {
            b.tvElapsed.setText("00:00:00");
            b.tvWorkoutInfo.setText("Pick a gym and a workout type, then start.");
            b.tvXpPreview.setText("");
        }
        updateCustomVisibility();
    }

    private void updateElapsed() {
        if (active == null) return;
        long elapsed = Math.max(0, System.currentTimeMillis() - active.startMs);
        long sec = elapsed / 1000;
        b.tvElapsed.setText(String.format(Locale.US, "%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60));
        updateXpPreview();
    }

    private void updateXpPreview() {
        if (active == null) {
            b.tvXpPreview.setText("");
            return;
        }
        int minutes = elapsedMinutes();
        int xp = XpCalculator.workoutXp(active.type, minutes);
        b.tvXpPreview.setText(xp > 0 ? "Worth +" + xp + " XP so far"
                : "Keep going: " + XpCalculator.MIN_WORKOUT_MINUTES + " min needed for XP");
    }

    private int elapsedMinutes() {
        return active == null ? 0 : (int) ((System.currentTimeMillis() - active.startMs) / 60000L);
    }

    private void confirmFinish() {
        if (active == null || saving) return;
        final int minutes = elapsedMinutes();
        final int xp = XpCalculator.workoutXp(active.type, minutes);
        if (xp <= 0) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Workout too short")
                    .setMessage("You've trained for " + minutes + " min. Workouts under "
                            + XpCalculator.MIN_WORKOUT_MINUTES + " minutes don't earn XP.")
                    .setPositiveButton("Keep going", null)
                    .setNegativeButton("Discard workout", (d, w) -> discard())
                    .show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm workout")
                .setMessage("Gym: " + active.gymName + "\nType: " + active.type.label
                        + "\nDuration: " + minutes + " min\nReward: +" + xp + " XP")
                .setPositiveButton("Confirm", (d, w) -> submit(minutes))
                .setNegativeButton("Keep going", null)
                .show();
    }

    private void submit(final int minutes) {
        final WorkoutSessionStore.Session s = active;
        if (s == null || saving) return;
        saving = true;
        b.btnFinishWorkout.setEnabled(false);
        Async.run(() -> sessions.completeWorkout(s.sessionId, s.gymName, s.type, minutes), (result, err) -> {
            saving = false;
            b.btnFinishWorkout.setEnabled(true);
            if (err != null || result == null) {
                Toast.makeText(this, "Couldn't save workout. Try again.", Toast.LENGTH_LONG).show();
                return;
            }
            // Granted, or already claimed: the session is over either way.
            store.clear();
            active = null;
            renderState();
            RewardUi.show(this, result);
        });
    }

    private void confirmCancel() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel workout?")
                .setMessage("No XP will be awarded.")
                .setPositiveButton("Cancel workout", (d, w) -> discard())
                .setNegativeButton("Keep going", null)
                .show();
    }

    private void discard() {
        store.clear();
        active = null;
        renderState();
    }
}
