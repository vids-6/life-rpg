package com.example.liferpg.ui;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

/** Small reusable animations. */
public final class Anim {
    private Anim() { }

    /** Smoothly animate a ProgressBar (max 1000) to the target. */
    public static void progress(ProgressBar bar, int target) {
        int clamped = Math.max(0, Math.min(bar.getMax(), target));
        ObjectAnimator animator = ObjectAnimator.ofInt(bar, "progress", bar.getProgress(), clamped);
        animator.setDuration(600);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    public static void fadeInUp(View view, long delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(36f);
        view.animate().alpha(1f).translationY(0f).setStartDelay(delayMs).setDuration(380).start();
    }

    public static void pop(final View view) {
        view.animate().scaleX(1.15f).scaleY(1.15f).setDuration(110).withEndAction(new Runnable() {
            @Override
            public void run() {
                view.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
            }
        }).start();
    }

    /** Quick gold pulse on a card when its quest is completed. */
    public static void completeFlash(final View view) {
        view.animate().scaleX(1.03f).scaleY(1.03f).setDuration(140).withEndAction(new Runnable() {
            @Override
            public void run() {
                view.animate().scaleX(1f).scaleY(1f).setDuration(220).start();
            }
        }).start();
    }
}
