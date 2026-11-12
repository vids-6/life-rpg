package com.example.liferpg.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A gym found through OpenStreetMap (or restored from the saved-gyms table).
 * {@link #placeId} is the stable identifier, e.g. {@code osm:node:123456}; it is also the primary key of the saved table.
 */
public class Gym {
    public String placeId = "";
    public String name = "";
    public String address = "";
    public double lat;
    public double lng;
    /** Short kind label such as "Fitness centre"; empty when unknown (e.g. restored from older saves). */
    public String category = "";
    /** Raw OSM opening_hours value, empty when not mapped. */
    public String openingHours = "";
    public String phone = "";
    public String website = "";
    /** Kept for backwards compatibility with gyms saved from the old Google Places version; -1 = none. */
    public double rating = -1;
    public int ratingCount;
    /** null when opening hours are unknown. */
    public Boolean openNow;
    public List<String> weekdayHours = new ArrayList<>();
    /** Metres from the reference point, -1 when unknown. */
    public float distanceMeters = -1f;
}
