package com.example.liferpg.ui;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.example.liferpg.R;
import com.example.liferpg.databinding.ActivityAvatarEditorBinding;

/**
 * Avatar studio. Works on a copy of the saved look: every tap updates the live preview, and the
 * look is only written to local storage when the player presses Save. Leaving with unsaved edits
 * asks for confirmation, so a stray back-press never loses (or silently changes) the hero.
 */
public class AvatarEditorActivity extends AppCompatActivity {
    private static final String STATE_CONFIG = "cfg";
    private static final String STATE_CATEGORY = "cat";

    private ActivityAvatarEditorBinding b;
    private AvatarConfig saved;
    private AvatarConfig config;
    private int category = AvatarConfig.SKIN;
    private View[] cells = new View[0];
    private TextView[] cellLabels = new TextView[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAvatarEditorBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        saved = Avatars.load(this);
        config = saved.copy();
        if (savedInstanceState != null) {
            config = AvatarConfig.parse(savedInstanceState.getString(STATE_CONFIG));
            category = savedInstanceState.getInt(STATE_CATEGORY, AvatarConfig.SKIN);
        }

        b.ibBack.setOnClickListener(v -> attemptExit());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                attemptExit();
            }
        });
        b.btnDone.setOnClickListener(v -> {
            Avatars.save(this, config);
            saved = config.copy();
            Toast.makeText(this, "Hero look saved", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });
        b.btnRandom.setOnClickListener(v -> {
            config = AvatarConfig.random();
            onConfigChanged(true);
        });
        b.btnReset.setOnClickListener(v -> {
            config = new AvatarConfig();
            onConfigChanged(true);
        });

        for (int tab : AvatarConfig.TAB_ORDER) {
            final int cat = tab;
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip, b.cgCategory, false);
            chip.setText(AvatarConfig.CATEGORY_LABELS[cat]);
            chip.setId(View.generateViewId());
            chip.setChecked(cat == category);
            chip.setOnClickListener(v -> {
                category = cat;
                buildOptions();
            });
            b.cgCategory.addView(chip);
        }
        b.avatarPreview.setConfig(config);
        buildOptions();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString(STATE_CONFIG, config.serialize());
        out.putInt(STATE_CATEGORY, category);
    }

    private boolean hasUnsavedChanges() {
        return !config.equals(saved);
    }

    private void attemptExit() {
        if (!hasUnsavedChanges()) {
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Discard changes?")
                .setMessage("Your new look has not been saved yet.")
                .setNegativeButton("Keep editing", null)
                .setPositiveButton("Discard", (d, w) -> finish())
                .show();
    }

    private void onConfigChanged(boolean animate) {
        b.avatarPreview.setConfig(config);
        if (animate) Anim.pop(b.avatarPreview);
        buildOptions();
    }

    // ------------------------------------------------------------------ option grid

    private boolean isSwatchCategory(int cat) {
        return cat == AvatarConfig.SKIN || cat == AvatarConfig.HAIR_COLOR;
    }

    /** Face-part thumbnails are cropped in on the relevant area so the differences are visible. */
    private float[] cropFor(int cat) {
        switch (cat) {
            case AvatarConfig.EYES: return new float[]{2.1f, 50f, 47f};
            case AvatarConfig.MOUTH: return new float[]{2.1f, 50f, 53f};
            case AvatarConfig.EXTRAS: return new float[]{1.8f, 50f, 52f};
            case AvatarConfig.HAIR_STYLE: return new float[]{1.35f, 50f, 40f};
            case AvatarConfig.ACCESSORY: return new float[]{1.4f, 50f, 38f};
            default: return new float[]{1f, 50f, 50f};
        }
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void buildOptions() {
        b.tvCategory.setText(AvatarConfig.CATEGORY_LABELS[category]);
        updateOptionName();
        GridLayout grid = b.gridOptions;
        grid.removeAllViews();
        final int n = AvatarConfig.count(category);
        final boolean swatches = isSwatchCategory(category);
        grid.setColumnCount(swatches ? 4 : 3);
        cells = new View[n];
        cellLabels = new TextView[n];
        float[] crop = cropFor(category);

        for (int i = 0; i < n; i++) {
            final int value = i;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            int pad = dp(swatches ? 12 : 8);
            cell.setPadding(pad, pad, pad, pad);

            if (swatches) {
                int color = category == AvatarConfig.SKIN ? AvatarPainter.SKIN[i] : AvatarPainter.HAIR[i];
                View dot = new View(this);
                GradientDrawable g = new GradientDrawable();
                g.setShape(GradientDrawable.OVAL);
                g.setColor(color);
                g.setStroke(dp(1), getColor(R.color.lr_stroke_strong));
                dot.setBackground(g);
                cell.addView(dot, new LinearLayout.LayoutParams(dp(44), dp(44)));
            } else {
                AvatarView av = new AvatarView(this);
                AvatarConfig variant = config.copy();
                variant.set(category, i);
                av.setConfig(variant);
                av.setZoom(crop[0], crop[1], crop[2]);
                cell.addView(av, new LinearLayout.LayoutParams(dp(80), dp(80)));

                TextView label = new TextView(this);
                label.setText(AvatarConfig.nameOf(category, i));
                label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(2);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(6);
                cell.addView(label, lp);
                cellLabels[i] = label;
            }

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            lp.setMargins(dp(5), dp(5), dp(5), dp(5));
            cell.setLayoutParams(lp);
            cell.setContentDescription(AvatarConfig.CATEGORY_LABELS[category] + ": "
                    + AvatarConfig.nameOf(category, i));
            cell.setOnClickListener(v -> {
                if (config.get(category) == value) return;
                config.set(category, value);
                b.avatarPreview.setConfig(config);
                Anim.pop(b.avatarPreview);
                updateSelection();
                updateOptionName();
            });
            cells[i] = cell;
            grid.addView(cell);
        }
        updateSelection();
    }

    private void updateSelection() {
        int selected = config.get(category);
        for (int i = 0; i < cells.length; i++) {
            boolean on = i == selected;
            cells[i].setBackgroundResource(on ? R.drawable.bg_option_selected : R.drawable.bg_option);
            cells[i].setSelected(on);
            if (cellLabels[i] != null) {
                cellLabels[i].setTextColor(getColor(on ? R.color.lr_gold_text : R.color.lr_text_secondary));
            }
        }
    }

    private void updateOptionName() {
        b.tvOptionName.setText(AvatarConfig.nameOf(category, config.get(category)));
    }
}
