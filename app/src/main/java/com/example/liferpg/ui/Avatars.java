package com.example.liferpg.ui;

import android.content.Context;

import com.example.liferpg.util.AppPrefs;

/** Avatar helpers: loads the saved look so every screen shows the same hero. */
public final class Avatars {
    /** Legacy emoji list kept so older profiles (avatarIndex) still map to a sensible default look. */
    public static final String[] ALL = {"🧙", "⚔️", "🏹", "🛡️", "🧝", "🐉"};

    private Avatars() { }

    public static String get(int index) {
        if (index < 0 || index >= ALL.length) return ALL[0];
        return ALL[index];
    }

    /** Saved customisation, or a default derived from the legacy avatar index. */
    public static AvatarConfig load(Context ctx, int legacyIndex) {
        String saved = new AppPrefs(ctx).getAvatarConfig();
        return saved == null ? AvatarConfig.fromLegacyIndex(legacyIndex) : AvatarConfig.parse(saved);
    }

    /** Same as {@link #load(Context, int)} for screens that do not know the legacy index. */
    public static AvatarConfig load(Context ctx) {
        return load(ctx, 0);
    }

    public static void save(Context ctx, AvatarConfig config) {
        new AppPrefs(ctx).setAvatarConfig(config.serialize());
    }

    /** Fills a header avatar and makes it open the editor on tap. */
    public static void bindHeader(final AvatarView view) {
        view.setConfig(load(view.getContext()));
        view.setOnClickListener(v -> v.getContext().startActivity(
                new android.content.Intent(v.getContext(), AvatarEditorActivity.class)));
    }
}
