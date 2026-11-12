package com.example.liferpg.ui;

import java.util.Arrays;
import java.util.Random;

/**
 * Avatar customisation choices. Stored locally as a compact comma-separated string of option
 * indexes (see {@link #serialize()}), so no database migration is needed and the look survives
 * app updates. Unknown / missing / out-of-range values fall back to defaults, which keeps older
 * saved strings (9 values) working after new categories were appended.
 */
public final class AvatarConfig {
    // Category ids. The numeric value is also the position inside the serialized string,
    // so NEVER reorder these; only append new ones.
    public static final int SKIN = 0;
    public static final int HAIR_STYLE = 1;
    public static final int HAIR_COLOR = 2;
    public static final int EYES = 3;
    public static final int MOUTH = 4;
    public static final int OUTFIT = 5;
    public static final int ACCESSORY = 6;
    public static final int BACKGROUND = 7;
    public static final int FRAME = 8;
    public static final int EXTRAS = 9;
    public static final int CATEGORY_COUNT = 10;

    /** Order in which the editor shows its tabs. */
    public static final int[] TAB_ORDER = {SKIN, HAIR_STYLE, HAIR_COLOR, EYES, MOUTH, EXTRAS,
            OUTFIT, ACCESSORY, BACKGROUND, FRAME};

    public static final String[] CATEGORY_LABELS = {"Skin", "Hair", "Hair colour", "Eyes", "Mouth",
            "Outfit", "Accessory", "Backdrop", "Frame", "Details"};

    public static final String[] SKIN_NAMES = {"Porcelain", "Fair", "Tan", "Olive", "Brown", "Deep"};
    public static final String[] HAIR_NAMES = {"Bald", "Short", "Long", "Spiky", "Bun", "Curly"};
    public static final String[] HAIR_COLOR_NAMES = {"Black", "Brown", "Auburn", "Blonde", "Silver", "Violet", "Teal"};
    public static final String[] EYE_NAMES = {"Classic", "Happy", "Bright", "Focused"};
    public static final String[] MOUTH_NAMES = {"Smile", "Grin", "Smirk", "Calm"};
    public static final String[] OUTFIT_NAMES = {"Tunic", "Armor", "Mage Robe", "Hoodie", "Ranger", "Royal"};
    public static final String[] ACCESSORY_NAMES = {"None", "Glasses", "Crown", "Headband", "Wizard Hat"};
    public static final String[] BG_NAMES = {"Twilight", "Ocean", "Sunset", "Forest", "Night", "Rose"};
    public static final String[] FRAME_NAMES = {"None", "Gold", "Arcane", "Silver", "Emerald"};
    public static final String[] EXTRA_NAMES = {"None", "Freckles", "Beard", "Mustache", "Scar"};

    private static final String[][] ALL_NAMES = {SKIN_NAMES, HAIR_NAMES, HAIR_COLOR_NAMES, EYE_NAMES,
            MOUTH_NAMES, OUTFIT_NAMES, ACCESSORY_NAMES, BG_NAMES, FRAME_NAMES, EXTRA_NAMES};

    public int skin = 1, hairStyle = 1, hairColor = 1, eyes = 0, mouth = 0,
            outfit = 0, accessory = 0, bg = 0, frame = 1, extras = 0;

    public AvatarConfig copy() {
        AvatarConfig c = new AvatarConfig();
        for (int i = 0; i < CATEGORY_COUNT; i++) c.set(i, get(i));
        return c;
    }

    public static int count(int category) {
        return ALL_NAMES[clamp(category)].length;
    }

    public static String nameOf(int category, int option) {
        String[] names = ALL_NAMES[clamp(category)];
        return names[Math.max(0, Math.min(names.length - 1, option))];
    }

    private static int clamp(int category) {
        return Math.max(0, Math.min(CATEGORY_COUNT - 1, category));
    }

    public int get(int category) {
        switch (category) {
            case SKIN: return skin;
            case HAIR_STYLE: return hairStyle;
            case HAIR_COLOR: return hairColor;
            case EYES: return eyes;
            case MOUTH: return mouth;
            case OUTFIT: return outfit;
            case ACCESSORY: return accessory;
            case BACKGROUND: return bg;
            case FRAME: return frame;
            default: return extras;
        }
    }

    public void set(int category, int v) {
        switch (category) {
            case SKIN: skin = v; break;
            case HAIR_STYLE: hairStyle = v; break;
            case HAIR_COLOR: hairColor = v; break;
            case EYES: eyes = v; break;
            case MOUTH: mouth = v; break;
            case OUTFIT: outfit = v; break;
            case ACCESSORY: accessory = v; break;
            case BACKGROUND: bg = v; break;
            case FRAME: frame = v; break;
            default: extras = v; break;
        }
    }

    public static AvatarConfig random() {
        Random r = new Random();
        AvatarConfig c = new AvatarConfig();
        for (int i = 0; i < CATEGORY_COUNT; i++) c.set(i, r.nextInt(count(i)));
        // Keep random looks tidy: most of the time no extra face detail and a visible frame.
        if (r.nextInt(3) != 0) c.extras = 0;
        if (c.frame == 0 && r.nextBoolean()) c.frame = 1 + r.nextInt(count(FRAME) - 1);
        return c;
    }

    /** Starting look derived from the legacy emoji avatar index so old profiles get a sensible default. */
    public static AvatarConfig fromLegacyIndex(int index) {
        AvatarConfig c = new AvatarConfig();
        c.outfit = Math.abs(index) % OUTFIT_NAMES.length;
        c.bg = Math.abs(index) % BG_NAMES.length;
        return c;
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CATEGORY_COUNT; i++) {
            if (i > 0) sb.append(',');
            sb.append(get(i));
        }
        return sb.toString();
    }

    public static AvatarConfig parse(String s) {
        AvatarConfig c = new AvatarConfig();
        if (s == null || s.isEmpty()) return c;
        String[] parts = s.split(",");
        for (int i = 0; i < CATEGORY_COUNT && i < parts.length; i++) {
            try {
                int v = Integer.parseInt(parts[i].trim());
                if (v >= 0 && v < count(i)) c.set(i, v);
            } catch (NumberFormatException ignored) {
                // keep default for this slot
            }
        }
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AvatarConfig)) return false;
        AvatarConfig other = (AvatarConfig) o;
        for (int i = 0; i < CATEGORY_COUNT; i++) if (get(i) != other.get(i)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int[] v = new int[CATEGORY_COUNT];
        for (int i = 0; i < CATEGORY_COUNT; i++) v[i] = get(i);
        return Arrays.hashCode(v);
    }
}
