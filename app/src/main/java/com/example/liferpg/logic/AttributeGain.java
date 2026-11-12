package com.example.liferpg.logic;

import com.example.liferpg.model.Attribute;

/** Attribute points granted by one reward. */
public final class AttributeGain {
    private final int[] values = new int[Attribute.values().length];

    public AttributeGain add(Attribute attribute, int amount) {
        values[attribute.ordinal()] += amount;
        return this;
    }

    public int get(Attribute attribute) {
        return values[attribute.ordinal()];
    }

    public int total() {
        int t = 0;
        for (int v : values) t += v;
        return t;
    }
}
