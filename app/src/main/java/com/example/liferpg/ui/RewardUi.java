package com.example.liferpg.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.liferpg.R;
import com.example.liferpg.model.Achievement;
import com.example.liferpg.model.RewardResult;

import java.util.Random;

/** Shows the outcome of a reward: a floating "+XP" pill for plain XP, an animated dialog for level-ups/achievements. */
public final class RewardUi {
    private RewardUi() { }

    public static void show(Context context, RewardResult result) {
        if (result == null) return;
        if (!result.awarded) {
            Toast.makeText(context, result.note, Toast.LENGTH_SHORT).show();
            return;
        }
        if (result.leveledUp() || !result.achievements.isEmpty()) {
            showCelebration(context, result);
        } else {
            showXpPill(context, result.xpText());
        }
    }

    private static Activity findActivity(Context c) {
        while (c instanceof ContextWrapper) {
            if (c instanceof Activity) return (Activity) c;
            c = ((ContextWrapper) c).getBaseContext();
        }
        return null;
    }

    /** Lightweight floating pill that rises and fades. */
    private static void showXpPill(Context context, String text) {
        Activity a = findActivity(context);
        if (a == null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            return;
        }
        final ViewGroup root = a.findViewById(android.R.id.content);
        final TextView pill = new TextView(a);
        pill.setText(text);
        pill.setTextColor(ContextCompat.getColor(a, R.color.lr_on_gold));
        pill.setTextSize(16);
        pill.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(a, R.font.poppins_semibold));
        pill.setBackgroundResource(R.drawable.bg_xp_toast);
        pill.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_bolt, 0, 0, 0);
        pill.setCompoundDrawableTintList(ColorStateList.valueOf(ContextCompat.getColor(a, R.color.lr_on_gold)));
        float d = a.getResources().getDisplayMetrics().density;
        pill.setCompoundDrawablePadding((int) (6 * d));
        pill.setPadding((int) (18 * d), (int) (10 * d), (int) (20 * d), (int) (10 * d));
        pill.setElevation(12 * d);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        lp.bottomMargin = (int) (120 * d);
        root.addView(pill, lp);
        pill.setAlpha(0f);
        pill.setScaleX(0.6f);
        pill.setScaleY(0.6f);
        pill.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220)
                .setInterpolator(new OvershootInterpolator()).withEndAction(() ->
                pill.animate().translationY(-90 * d).alpha(0f).setStartDelay(650).setDuration(550)
                        .withEndAction(() -> root.removeView(pill)).start()).start();
    }

    private static void showCelebration(Context context, RewardResult result) {
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_reward, null);
        ImageView icon = v.findViewById(R.id.iv_reward_icon);
        AvatarView avatar = v.findViewById(R.id.avatar_reward);
        TextView title = v.findViewById(R.id.tv_reward_title);
        TextView msg = v.findViewById(R.id.tv_reward_msg);
        final boolean level = result.leveledUp();
        final View hero;
        if (level) {
            // Level-up: show the player's own customised hero.
            avatar.setConfig(Avatars.load(context));
            avatar.setVisibility(View.VISIBLE);
            icon.setVisibility(View.GONE);
            hero = avatar;
        } else {
            icon.setImageResource(Icons.achievement(result.achievements.get(0)));
            hero = icon;
        }
        title.setText(level ? "LEVEL " + result.newLevel : "Achievement!");
        StringBuilder sb = new StringBuilder(result.xpText());
        if (level) sb.append("\nYou reached level ").append(result.newLevel);
        for (Achievement a : result.achievements) {
            sb.append("\n").append(a.title);
        }
        msg.setText(sb.toString());

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(v);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int) (context.getResources().getDisplayMetrics().widthPixels * 0.88f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        v.findViewById(R.id.btn_reward_ok).setOnClickListener(x -> dialog.dismiss());
        dialog.show();

        // Pop-in + sparkles.
        View card = v.findViewById(R.id.reward_card);
        card.setScaleX(0.7f);
        card.setScaleY(0.7f);
        card.setAlpha(0f);
        card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(320).setInterpolator(new OvershootInterpolator()).start();
        hero.setScaleX(0f);
        hero.setScaleY(0f);
        hero.animate().scaleX(1f).scaleY(1f).setStartDelay(180).setDuration(450).setInterpolator(new OvershootInterpolator(2.5f)).start();
        burst((FrameLayout) v, context);
    }

    /** Gold star / sparkle vector particles flying outwards from the hero. */
    private static void burst(final FrameLayout host, final Context context) {
        final float d = context.getResources().getDisplayMetrics().density;
        final Random r = new Random();
        final ColorStateList gold = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.lr_gold_light));
        host.post(() -> {
            float cx = host.getWidth() / 2f, cy = host.getHeight() * 0.24f;
            for (int i = 0; i < 14; i++) {
                final ImageView t = new ImageView(host.getContext());
                t.setImageResource(i % 3 == 0 ? R.drawable.ic_star_filled : R.drawable.ic_sparkle);
                t.setImageTintList(gold);
                int size = (int) ((12 + r.nextInt(10)) * d);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
                host.addView(t, lp);
                t.setX(cx - size / 2f);
                t.setY(cy - size / 2f);
                double ang = r.nextDouble() * Math.PI * 2;
                float dist = (60 + r.nextInt(90)) * d;
                t.animate().translationXBy((float) Math.cos(ang) * dist).translationYBy((float) Math.sin(ang) * dist)
                        .alpha(0f).rotation(r.nextInt(240) - 120).setStartDelay(150).setDuration(900 + r.nextInt(400))
                        .withEndAction(() -> host.removeView(t)).start();
            }
        });
    }
}
