package com.example.liferpg.service;

import android.util.LruCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.liferpg.BuildConfig;
import com.example.liferpg.model.Gym;

/**
 * Gym discovery on OpenStreetMap data. No API key, account or Google service is needed.
 *
 * <ul>
 *   <li><b>Overpass API</b> ({@code overpass-api.de}, with a public mirror as fallback): gyms around a point
 *       and gyms by name around a point.</li>
 *   <li><b>Nominatim</b> ({@code nominatim.openstreetmap.org}): turns a place name into coordinates and
 *       searches gyms by name when no position is known.</li>
 * </ul>
 *
 * All methods block and must be called from a background thread ({@code Async.network}). Both services are
 * free public infrastructure, so the client is deliberately polite: an identifying User-Agent, at most one
 * request per second per host, connect/read timeouts, a mirror fallback, a small in-memory result cache, and
 * clear error kinds for timeouts and rate limits (HTTP 429) so the UI can offer a retry.
 */
public class OsmGymService {

    public enum ErrorKind { NETWORK, TIMEOUT, RATE_LIMIT, SERVER, NOT_FOUND, OTHER }

    public static class OsmException extends Exception {
        public final ErrorKind kind;

        public OsmException(ErrorKind kind, String message) {
            super(message);
            this.kind = kind;
        }
    }

    /** A geocoded place. */
    public static class Place {
        public double lat;
        public double lng;
        public String label = "";
    }

    // Not final only so the JVM test harness can point the service at a local fake server.
    static String[] OVERPASS = {
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter"
    };
    static String NOMINATIM = "https://nominatim.openstreetmap.org/search";

    static int CONNECT_TIMEOUT_MS = 10_000;
    static int READ_TIMEOUT_MS = 25_000;
    private static final long MIN_GAP_MS = 1100;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;
    private static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;
    private static final int MAX_RESULTS = 60;

    private static final String USER_AGENT = "LifeRPG/" + BuildConfig.VERSION_NAME
            + " (Android; " + BuildConfig.APPLICATION_ID + ")";

    private static final Object THROTTLE_LOCK = new Object();
    private static long lastOverpassAt;
    private static long lastNominatimAt;

    private static final class CacheEntry {
        final long at = System.currentTimeMillis();
        final List<Gym> gyms;

        CacheEntry(List<Gym> gyms) {
            this.gyms = gyms;
        }
    }

    private static final LruCache<String, CacheEntry> CACHE = new LruCache<>(24);

    // ------------------------------------------------------------------ public API

    /** Gyms within radiusMeters of the point. Unsorted; the caller sorts by distance. */
    public List<Gym> searchNearby(double lat, double lng, int radiusMeters) throws OsmException {
        int r = Math.max(200, Math.min(radiusMeters, 25000));
        String key = String.format(Locale.US, "near:%.3f:%.3f:%d", lat, lng, r);
        List<Gym> cached = fromCache(key);
        if (cached != null) return cached;

        String around = String.format(Locale.US, "(around:%d,%.6f,%.6f)", r, lat, lng);
        String q = "[out:json][timeout:20];("
                + "nwr[\"leisure\"=\"fitness_centre\"]" + around + ";"
                + "nwr[\"amenity\"=\"gym\"]" + around + ";"
                + "nwr[\"leisure\"=\"sports_centre\"][\"sport\"~\"fitness|gym|weightlifting|crossfit|bodybuilding\"]" + around + ";"
                + ");out tags center 200;";
        List<Gym> gyms = parseOverpass(overpass(q));
        toCache(key, gyms);
        return new ArrayList<>(gyms);
    }

    /**
     * Gyms whose name matches the text. With a reference point the search covers 30 km around it (Overpass) and
     * is merged with a global Nominatim search; without one only Nominatim is used.
     */
    public List<Gym> searchByName(String text, boolean hasRef, double lat, double lng) throws OsmException {
        String clean = text.trim();
        String key = String.format(Locale.US, "name:%s:%s", clean.toLowerCase(Locale.ROOT),
                hasRef ? String.format(Locale.US, "%.2f:%.2f", lat, lng) : "-");
        List<Gym> cached = fromCache(key);
        if (cached != null) return cached;

        List<Gym> merged = new ArrayList<>();
        OsmException failure = null;
        boolean anySuccess = false;

        if (hasRef) {
            try {
                String re = regexFor(clean);
                String around = String.format(Locale.US, "(around:30000,%.6f,%.6f)", lat, lng);
                String q = "[out:json][timeout:20];("
                        + "nwr[\"leisure\"~\"fitness_centre|sports_centre\"][\"name\"~\"" + re + "\",i]" + around + ";"
                        + "nwr[\"amenity\"=\"gym\"][\"name\"~\"" + re + "\",i]" + around + ";"
                        + ");out tags center 100;";
                merged.addAll(parseOverpass(overpass(q)));
                anySuccess = true;
            } catch (OsmException e) {
                failure = e;
            }
        }
        try {
            List<Gym> found = nominatimGyms(clean, hasRef, lat, lng);
            if (found.isEmpty() && !clean.toLowerCase(Locale.ROOT).contains("gym")) {
                found = nominatimGyms(clean + " gym", hasRef, lat, lng);
            }
            addAllUnique(merged, found);
            anySuccess = true;
        } catch (OsmException e) {
            if (failure == null) failure = e;
        }
        if (!anySuccess && failure != null) throw failure;
        dedupe(merged);
        toCache(key, merged);
        return new ArrayList<>(merged);
    }

    /** Turns "Koramangala, Bengaluru" into coordinates. Throws NOT_FOUND when nothing matches. */
    public Place geocode(String text) throws OsmException {
        String url = NOMINATIM + "?format=jsonv2&limit=1&accept-language=" + lang() + "&q=" + enc(text.trim());
        JSONArray arr = parseArray(request("GET", url, null, Host.NOMINATIM));
        if (arr.length() == 0) {
            throw new OsmException(ErrorKind.NOT_FOUND, "Couldn't find a place called \"" + text.trim() + "\".");
        }
        JSONObject o = arr.optJSONObject(0);
        if (o == null) throw new OsmException(ErrorKind.NOT_FOUND, "Couldn't find that place.");
        Place p = new Place();
        p.lat = o.optDouble("lat", Double.NaN);
        p.lng = o.optDouble("lon", Double.NaN);
        if (Double.isNaN(p.lat) || Double.isNaN(p.lng)) {
            throw new OsmException(ErrorKind.NOT_FOUND, "Couldn't find that place.");
        }
        String name = o.optString("name", "");
        p.label = name.isEmpty() ? shorten(o.optString("display_name", text)) : name;
        return p;
    }

    // ------------------------------------------------------------------ Overpass

    private String overpass(String query) throws OsmException {
        String body = "data=" + enc(query);
        OsmException last = null;
        for (String endpoint : OVERPASS) {
            try {
                String json = request("POST", endpoint, body, Host.OVERPASS);
                // Overpass reports some failures with HTTP 200 and a "remark" instead of elements.
                JSONObject o = new JSONObject(json);
                String remark = o.optString("remark", "");
                JSONArray els = o.optJSONArray("elements");
                if (!remark.isEmpty() && (els == null || els.length() == 0)
                        && (remark.contains("timed out") || remark.contains("out of memory") || remark.contains("runtime error"))) {
                    last = new OsmException(ErrorKind.SERVER, "The map server took too long to answer.");
                    continue;
                }
                return json;
            } catch (JSONException e) {
                last = new OsmException(ErrorKind.SERVER, "The map server sent an unexpected answer.");
            } catch (OsmException e) {
                last = e;
                if (e.kind == ErrorKind.OTHER) throw e; // client-side problem, a mirror will not help
            }
        }
        throw last != null ? last : new OsmException(ErrorKind.OTHER, "Gym search failed.");
    }

    private List<Gym> parseOverpass(String json) throws OsmException {
        List<Gym> out = new ArrayList<>();
        try {
            JSONArray els = new JSONObject(json).optJSONArray("elements");
            if (els == null) return out;
            for (int i = 0; i < els.length(); i++) {
                JSONObject e = els.optJSONObject(i);
                if (e == null) continue;
                String type = e.optString("type", "node");
                long id = e.optLong("id", 0);
                double lat;
                double lng;
                if (e.has("lat") && e.has("lon")) {
                    lat = e.optDouble("lat");
                    lng = e.optDouble("lon");
                } else {
                    JSONObject c = e.optJSONObject("center");
                    if (c == null) continue;
                    lat = c.optDouble("lat");
                    lng = c.optDouble("lon");
                }
                JSONObject tags = e.optJSONObject("tags");
                if (id == 0 || tags == null || Double.isNaN(lat) || Double.isNaN(lng)) continue;

                Gym g = new Gym();
                g.placeId = "osm:" + type + ":" + id;
                g.name = firstNonEmpty(tags.optString("name"), tags.optString("brand"), tags.optString("operator"));
                if (g.name.isEmpty()) continue; // an unnamed gym is not useful to the player
                g.lat = lat;
                g.lng = lng;
                g.address = addressFromTags(tags);
                g.category = categoryLabel(tags.optString("leisure"), tags.optString("amenity"));
                g.openingHours = tags.optString("opening_hours", "").trim();
                g.phone = firstNonEmpty(tags.optString("phone"), tags.optString("contact:phone"));
                g.website = firstNonEmpty(tags.optString("website"), tags.optString("contact:website"));
                if (!g.openingHours.isEmpty()) {
                    for (String part : g.openingHours.split(";")) {
                        String t = part.trim();
                        if (!t.isEmpty()) g.weekdayHours.add(t);
                    }
                }
                out.add(g);
            }
        } catch (JSONException e) {
            throw new OsmException(ErrorKind.SERVER, "The map server sent an unexpected answer.");
        }
        dedupe(out);
        return out;
    }

    // ------------------------------------------------------------------ Nominatim

    private List<Gym> nominatimGyms(String text, boolean hasRef, double lat, double lng) throws OsmException {
        StringBuilder url = new StringBuilder(NOMINATIM);
        url.append("?format=jsonv2&addressdetails=1&limit=30&accept-language=").append(lang());
        if (hasRef) {
            // Prefer (but do not require) results near the reference point: roughly +-1 degree.
            url.append(String.format(Locale.US, "&viewbox=%.4f,%.4f,%.4f,%.4f&bounded=0",
                    lng - 1.0, lat + 1.0, lng + 1.0, lat - 1.0));
        }
        url.append("&q=").append(enc(text));
        JSONArray arr = parseArray(request("GET", url.toString(), null, Host.NOMINATIM));
        List<Gym> out = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String cat = o.optString("category", "");
            String type = o.optString("type", "");
            String name = o.optString("name", "");
            if (!isGymLike(cat, type, name)) continue;
            double glat = o.optDouble("lat", Double.NaN);
            double glng = o.optDouble("lon", Double.NaN);
            String osmType = o.optString("osm_type", "");
            long osmId = o.optLong("osm_id", 0);
            if (Double.isNaN(glat) || Double.isNaN(glng) || osmId == 0 || osmType.isEmpty()) continue;
            if (name.isEmpty()) name = shorten(o.optString("display_name", ""));
            if (name.isEmpty()) continue;

            Gym g = new Gym();
            g.placeId = "osm:" + osmType + ":" + osmId;
            g.name = name;
            g.lat = glat;
            g.lng = glng;
            g.category = categoryLabel(cat.equals("leisure") ? type : "", cat.equals("amenity") ? type : "");
            JSONObject addr = o.optJSONObject("address");
            g.address = addr != null ? addressFromNominatim(addr, name) : "";
            if (g.address.isEmpty()) g.address = shorten(o.optString("display_name", ""));
            out.add(g);
        }
        return out;
    }

    private static boolean isGymLike(String category, String type, String name) {
        if ("leisure".equals(category)
                && ("fitness_centre".equals(type) || "sports_centre".equals(type) || "fitness_station".equals(type))) {
            return true;
        }
        if ("amenity".equals(category) && "gym".equals(type)) return true;
        String n = name.toLowerCase(Locale.ROOT);
        boolean nameSaysGym = n.contains("gym") || n.contains("fitness") || n.contains("crossfit") || n.contains("fit ");
        boolean placeIsPoi = "leisure".equals(category) || "amenity".equals(category) || "shop".equals(category)
                || "sport".equals(category) || "building".equals(category) || "office".equals(category);
        return nameSaysGym && placeIsPoi;
    }

    // ------------------------------------------------------------------ HTTP

    private enum Host { OVERPASS, NOMINATIM }

    private static void throttle(Host host) {
        // Reserve a time slot under the lock but sleep outside it, so a slow Nominatim slot never blocks Overpass.
        long wait;
        synchronized (THROTTLE_LOCK) {
            long now = System.currentTimeMillis();
            long last = host == Host.OVERPASS ? lastOverpassAt : lastNominatimAt;
            long slot = Math.max(now, last + MIN_GAP_MS);
            wait = slot - now;
            if (host == Host.OVERPASS) lastOverpassAt = slot;
            else lastNominatimAt = slot;
        }
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String request(String method, String url, String formBody, Host host) throws OsmException {
        throttle(host);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Accept-Language", lang());
            if (formBody != null) {
                byte[] bytes = formBody.getBytes(StandardCharsets.UTF_8);
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(bytes.length);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                }
            }
            int code = conn.getResponseCode();
            if (code == 200) {
                try {
                    return readAll(conn.getInputStream());
                } catch (TooLargeException e) {
                    throw new OsmException(ErrorKind.SERVER, "The map server sent too much data. Try a smaller radius.");
                }
            }
            if (code == 429) {
                throw new OsmException(ErrorKind.RATE_LIMIT,
                        "The free map server is busy right now (too many requests). Wait a few seconds and retry.");
            }
            if (code == 408 || code == 504) {
                throw new OsmException(ErrorKind.TIMEOUT, "The map server took too long to answer.");
            }
            if (code >= 500) {
                throw new OsmException(ErrorKind.SERVER, "The map server is having problems (HTTP " + code + ").");
            }
            throw new OsmException(ErrorKind.OTHER, "The map server refused the request (HTTP " + code + ").");
        } catch (SocketTimeoutException e) {
            throw new OsmException(ErrorKind.TIMEOUT, "The request timed out. Check your connection and retry.");
        } catch (IOException e) {
            throw new OsmException(ErrorKind.NETWORK, "Couldn't reach the map server. Check your internet connection.");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static final class TooLargeException extends IOException {
        TooLargeException() {
            super("response too large");
        }
    }

    private static String readAll(InputStream in) throws IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                bos.write(buf, 0, n);
                if (bos.size() > MAX_RESPONSE_BYTES) throw new TooLargeException();
            }
            return bos.toString("UTF-8");
        }
    }

    private static JSONArray parseArray(String json) throws OsmException {
        try {
            return new JSONArray(json);
        } catch (JSONException e) {
            throw new OsmException(ErrorKind.SERVER, "The map server sent an unexpected answer.");
        }
    }

    // ------------------------------------------------------------------ helpers

    private static List<Gym> fromCache(String key) {
        CacheEntry e;
        synchronized (CACHE) {
            e = CACHE.get(key);
            if (e != null && System.currentTimeMillis() - e.at > CACHE_TTL_MS) {
                CACHE.remove(key);
                e = null;
            }
        }
        return e == null ? null : new ArrayList<>(e.gyms);
    }

    private static void toCache(String key, List<Gym> gyms) {
        synchronized (CACHE) {
            CACHE.put(key, new CacheEntry(new ArrayList<>(gyms)));
        }
    }

    /** Case-insensitive Overpass regex for free text: letters, digits and spaces stay, anything else becomes "." */
    static String regexFor(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append(Character.isLetterOrDigit(c) || c == ' ' ? c : '.');
        }
        return sb.toString();
    }

    private static void addAllUnique(List<Gym> into, List<Gym> more) {
        for (Gym g : more) {
            boolean exists = false;
            for (Gym x : into) {
                if (x.placeId.equals(g.placeId)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) into.add(g);
        }
    }

    /** OSM often maps one gym as both a building outline and a point: drop same-name entries within 60 m. */
    private static void dedupe(List<Gym> gyms) {
        float[] res = new float[1];
        for (int i = 0; i < gyms.size(); i++) {
            Gym a = gyms.get(i);
            for (int j = gyms.size() - 1; j > i; j--) {
                Gym b = gyms.get(j);
                boolean same = a.placeId.equals(b.placeId);
                if (!same && a.name.equalsIgnoreCase(b.name)) {
                    android.location.Location.distanceBetween(a.lat, a.lng, b.lat, b.lng, res);
                    same = res[0] < 60f;
                }
                if (same) {
                    if (a.address.isEmpty() && !b.address.isEmpty()) a.address = b.address;
                    gyms.remove(j);
                }
            }
        }
        if (gyms.size() > MAX_RESULTS * 4) {
            gyms.subList(MAX_RESULTS * 4, gyms.size()).clear();
        }
    }

    static String categoryLabel(String leisure, String amenity) {
        if ("fitness_centre".equals(leisure)) return "Fitness centre";
        if ("sports_centre".equals(leisure)) return "Sports centre";
        if ("fitness_station".equals(leisure)) return "Outdoor fitness";
        if ("gym".equals(amenity)) return "Gym";
        return "Gym";
    }

    private static String addressFromTags(JSONObject t) {
        String full = t.optString("addr:full", "").trim();
        if (!full.isEmpty()) return full;
        List<String> parts = new ArrayList<>();
        String street = t.optString("addr:street", "").trim();
        String house = t.optString("addr:housenumber", "").trim();
        if (!street.isEmpty()) parts.add(house.isEmpty() ? street : house + " " + street);
        else if (!house.isEmpty()) parts.add(house);
        addIfPresent(parts, firstNonEmpty(t.optString("addr:suburb"), t.optString("addr:neighbourhood"),
                t.optString("addr:district")));
        addIfPresent(parts, firstNonEmpty(t.optString("addr:city"), t.optString("addr:town"),
                t.optString("addr:village"), t.optString("addr:place")));
        return join(parts);
    }

    private static String addressFromNominatim(JSONObject a, String name) {
        List<String> parts = new ArrayList<>();
        String road = a.optString("road", "").trim();
        String house = a.optString("house_number", "").trim();
        if (!road.isEmpty()) parts.add(house.isEmpty() ? road : house + " " + road);
        addIfPresent(parts, firstNonEmpty(a.optString("suburb"), a.optString("neighbourhood"), a.optString("quarter")));
        addIfPresent(parts, firstNonEmpty(a.optString("city"), a.optString("town"), a.optString("village"),
                a.optString("county")));
        return join(parts);
    }

    private static void addIfPresent(List<String> parts, String v) {
        if (v != null && !v.trim().isEmpty() && !parts.contains(v.trim())) parts.add(v.trim());
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(p);
        }
        return sb.toString();
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return "";
    }

    private static String shorten(String displayName) {
        if (displayName == null) return "";
        String[] bits = displayName.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bits.length && i < 3; i++) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(bits[i].trim());
        }
        return sb.toString();
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return s;
        }
    }

    private static String lang() {
        String l = Locale.getDefault().toLanguageTag();
        return l == null || l.isEmpty() || "und".equals(l) ? "en" : l;
    }
}
