package com.example.liferpg.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.example.liferpg.R;
import com.example.liferpg.databinding.ItemGymBinding;
import com.example.liferpg.model.Gym;

public class GymAdapter extends RecyclerView.Adapter<GymAdapter.Holder> {

    public interface Listener {
        void onOpen(Gym gym);

        void onDirections(Gym gym);

        void onToggleSave(Gym gym);
    }

    private final List<Gym> items = new ArrayList<>();
    private Set<String> savedIds = new HashSet<>();
    private final Listener listener;

    public GymAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Gym> gyms, Set<String> saved) {
        items.clear();
        items.addAll(gyms);
        savedIds = new HashSet<>(saved);
        notifyDataSetChanged();
    }

    /** Refreshes the Save/Unsave buttons without touching the list. */
    public void setSavedIds(Set<String> saved) {
        savedIds = new HashSet<>(saved);
        notifyDataSetChanged();
    }

    public static String formatDistance(float meters) {
        if (meters < 0) return "";
        if (meters < 1000) return Math.round(meters) + " m";
        return String.format(Locale.getDefault(), "%.1f km", meters / 1000f);
    }

    /** Address if OpenStreetMap has one, otherwise the coordinates, so every result shows where it is. */
    public static String formatLocation(Gym g) {
        if (g.address != null && !g.address.isEmpty()) return g.address;
        return String.format(Locale.getDefault(), "Location: %.4f, %.4f", g.lat, g.lng);
    }

    /** One line about opening hours, or empty when nothing is known. */
    public static String formatHours(Gym g) {
        if (g.openingHours == null || g.openingHours.isEmpty()) return "";
        if (g.openingHours.contains("24/7")) return "Open 24/7";
        return "Hours: " + g.openingHours;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemGymBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final Gym g = items.get(position);
        h.b.tvName.setText(g.name);
        h.b.tvCategory.setText(g.category);
        h.b.tvCategory.setVisibility(g.category == null || g.category.isEmpty() ? View.GONE : View.VISIBLE);
        h.b.tvAddress.setText(formatLocation(g));
        String dist = formatDistance(g.distanceMeters);
        h.b.tvDistance.setText(dist);
        h.b.tvDistance.setVisibility(dist.isEmpty() ? View.GONE : View.VISIBLE);
        String hours = formatHours(g);
        h.b.tvHours.setText(hours);
        h.b.tvHours.setVisibility(hours.isEmpty() ? View.GONE : View.VISIBLE);

        bindSaveButton(h, savedIds.contains(g.placeId));
        h.b.getRoot().setContentDescription(g.name + ", " + (dist.isEmpty() ? "" : dist + " away"));
        h.b.getRoot().setOnClickListener(v -> listener.onOpen(g));
        h.b.btnDirections.setOnClickListener(v -> listener.onDirections(g));
        h.b.btnSave.setOnClickListener(v -> listener.onToggleSave(g));
    }

    /** Saved = filled gold button "Unsave"; not saved = outlined "Save". Always clearly visible. */
    private void bindSaveButton(Holder h, boolean saved) {
        android.content.Context c = h.b.getRoot().getContext();
        if (saved) {
            h.b.btnSave.setText("Unsave");
            h.b.btnSave.setIconResource(R.drawable.ic_bookmark_filled);
            h.b.btnSave.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, R.color.lr_gold)));
            h.b.btnSave.setTextColor(ContextCompat.getColor(c, R.color.lr_on_gold));
            h.b.btnSave.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(c, R.color.lr_on_gold)));
            h.b.btnSave.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(c, R.color.lr_gold)));
            h.b.btnSave.setContentDescription("Unsave " + h.b.tvName.getText());
        } else {
            h.b.btnSave.setText("Save");
            h.b.btnSave.setIconResource(R.drawable.ic_bookmark);
            h.b.btnSave.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            h.b.btnSave.setTextColor(ContextCompat.getColor(c, R.color.lr_text));
            h.b.btnSave.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(c, R.color.lr_primary_text)));
            h.b.btnSave.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(c, R.color.lr_stroke_strong)));
            h.b.btnSave.setContentDescription("Save " + h.b.tvName.getText());
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemGymBinding b;

        Holder(ItemGymBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
