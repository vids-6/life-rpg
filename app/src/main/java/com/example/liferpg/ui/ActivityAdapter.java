package com.example.liferpg.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import com.example.liferpg.data.ActivityLog;
import com.example.liferpg.databinding.ItemActivityBinding;
import com.example.liferpg.logic.DateUtils;

/** Row binder for activity-history entries (used inside the Stats screen). */
public final class ActivityAdapter {
    private ActivityAdapter() { }

    public static String subtitle(ActivityLog log) {
        StringBuilder sb = new StringBuilder(DateUtils.formatDateTime(log.timestamp));
        if (log.durationMin > 0) sb.append(" • ").append(log.durationMin).append(" min");
        if (log.detail != null && !log.detail.isEmpty()) sb.append(" • ").append(log.detail);
        return sb.toString();
    }

    public static View createRow(LayoutInflater inflater, ViewGroup parent, ActivityLog log) {
        ItemActivityBinding b = ItemActivityBinding.inflate(inflater, parent, false);
        Icons.set(b.ivIcon, Icons.activity(log.type), Icons.activityColor(log.type));
        b.tvTitle.setText(log.title);
        b.tvSub.setText(subtitle(log));
        b.tvXp.setText("+" + log.xp + " XP");
        return b.getRoot();
    }

    /** Fills a container with up to max rows. */
    public static void fill(LayoutInflater inflater, ViewGroup container, List<ActivityLog> logs) {
        container.removeAllViews();
        for (ActivityLog log : new ArrayList<>(logs)) {
            container.addView(createRow(inflater, container, log));
        }
    }
}
