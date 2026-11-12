package com.example.liferpg.service;

import com.example.liferpg.BuildConfig;

/**
 * The Google Maps SDK key is now OPTIONAL: gym search itself uses OpenStreetMap data and needs no key.
 * When a key is present in local.properties the Gym Finder additionally shows an interactive map.
 */
public final class MapKey {
    private MapKey() { }

    public static boolean isConfigured() {
        String k = BuildConfig.MAPS_API_KEY == null ? "" : BuildConfig.MAPS_API_KEY.trim();
        return !k.isEmpty() && !k.startsWith("YOUR_");
    }
}
