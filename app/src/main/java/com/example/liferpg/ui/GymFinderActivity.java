package com.example.liferpg.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.liferpg.R;
import com.example.liferpg.data.SavedGym;
import com.example.liferpg.databinding.ActivityGymFinderBinding;
import com.example.liferpg.databinding.DialogGymBinding;
import com.example.liferpg.model.Gym;
import com.example.liferpg.repository.GymRepository;
import com.example.liferpg.service.LocationHelper;
import com.example.liferpg.service.MapKey;
import com.example.liferpg.service.OsmGymService;
import com.example.liferpg.util.Async;
import com.example.liferpg.util.NetworkUtils;

/**
 * In-app gym discovery on OpenStreetMap data, with persistent saved gyms.
 *
 * <ul>
 *   <li><b>Discover</b>: search by gym name, search an area, or find gyms near the device location.</li>
 *   <li><b>Saved</b>: favourites stored in Room, available offline.</li>
 * </ul>
 * Searching never leaves the app; only the explicit Directions button hands off to a maps app.
 * Every failure (offline, denied permission, timeout, rate limit, no results) lands in a recoverable state.
 */
public class GymFinderActivity extends AppCompatActivity implements OnMapReadyCallback, GymAdapter.Listener {
    private static final String KEY_MAP = "map_state";
    private static final String KEY_TAB = "tab";
    private static final String KEY_KIND = "kind";
    private static final String KEY_QUERY = "query";
    private static final String KEY_REF_LAT = "ref_lat";
    private static final String KEY_REF_LNG = "ref_lng";
    private static final String KEY_REF_LABEL = "ref_label";
    private static final String KEY_MODE_AREA = "mode_area";
    private static final String KEY_RADIUS = "radius";

    private static final int KIND_NONE = 0;
    private static final int KIND_NEAR = 1;
    private static final int KIND_AREA = 2;
    private static final int KIND_NAME = 3;

    private static final int MAX_SHOWN = 50;

    private ActivityGymFinderBinding b;
    private GoogleMap map;
    private boolean mapEnabled;

    private final OsmGymService osm = new OsmGymService();
    private LocationHelper locationHelper;
    private GymRepository gymRepo;
    private GymAdapter adapter;

    // location state
    private Location userLocation;
    private boolean hasRef;
    private double refLat;
    private double refLng;
    private String refLabel = "";
    private boolean askedPermission;

    // search state
    private int radiusM = 2000;
    private int lastKind = KIND_NONE;
    private String lastQuery = "";
    private int searchToken;
    private int locationPendingToken = -1;
    private static final long LOCATION_TIMEOUT_MS = 20_000;
    private boolean loading;
    private boolean permissionDeniedShown;
    /** True when distances are measured from the device, false when from a searched area. */
    private boolean originIsDevice;

    // what each tab shows
    private boolean showingSaved;
    private final List<Gym> discoverResults = new ArrayList<>();
    private final List<Gym> savedGyms = new ArrayList<>();
    private Set<String> savedIds = new HashSet<>();
    private Runnable discoverView;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), r -> onPermissionResult());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityGymFinderBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        locationHelper = new LocationHelper(this);
        gymRepo = new GymRepository(this);
        adapter = new GymAdapter(this);
        b.rvGyms.setLayoutManager(new LinearLayoutManager(this));
        b.rvGyms.setAdapter(adapter);

        setUpMap(savedInstanceState);
        setUpTabs();
        setUpControls();

        Bundle s = savedInstanceState;
        if (s != null) {
            radiusM = s.getInt(KEY_RADIUS, 2000);
            selectRadiusChip(radiusM);
            if (s.getBoolean(KEY_MODE_AREA)) b.chipModeLocation.setChecked(true);
            updateSearchHint();
        }

        // Saved gyms first: they decide the button state of every result card.
        Async.run(() -> gymRepo.savedIds(), (ids, err) -> {
            if (ids != null) savedIds = ids;
            updateTabCount();
            if (s != null) restoreSession(s);
            else startFresh();
        });
    }

    // ------------------------------------------------------------------ set-up

    private void setUpTabs() {
        b.tabs.addTab(b.tabs.newTab().setText("Discover"));
        b.tabs.addTab(b.tabs.newTab().setText("Saved"));
        b.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTab(tab.getPosition() == 1);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    private void setUpControls() {
        b.ibBack.setOnClickListener(v -> finish());
        b.btnSearch.setOnClickListener(v -> onSearchPressed());
        b.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                onSearchPressed();
                return true;
            }
            return false;
        });
        b.btnNearMe.setOnClickListener(v -> {
            hideKeyboard();
            findNearMe();
        });
        b.cgMode.setOnCheckedStateChangeListener((group, ids) -> {
            b.tilSearch.setError(null);
            updateSearchHint();
        });
        b.cgRadius.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            Chip c = group.findViewById(ids.get(0));
            radiusM = Integer.parseInt(String.valueOf(c.getTag()));
            // The radius only matters for "near me" and "area" searches: re-run the current one.
            if ((lastKind == KIND_NEAR && hasRef) || lastKind == KIND_AREA) rerunLast();
        });
        updateSearchHint();
    }

    private void updateSearchHint() {
        b.tilSearch.setHint(b.chipModeLocation.isChecked()
                ? "City, neighbourhood or address" : "Gym name, e.g. Gold's Gym");
    }

    private void setUpMap(Bundle savedInstanceState) {
        mapEnabled = MapKey.isConfigured();
        if (!mapEnabled) {
            b.cardMap.setVisibility(View.GONE);
            return;
        }
        b.cardMap.setVisibility(View.VISIBLE);
        Bundle mapState = savedInstanceState == null ? null : savedInstanceState.getBundle(KEY_MAP);
        b.mapView.onCreate(mapState);
        b.mapView.getMapAsync(this);
    }

    private void selectRadiusChip(int radius) {
        Chip[] chips = {b.chipR1, b.chipR2, b.chipR5, b.chipR10};
        for (Chip c : chips) {
            if (Integer.parseInt(String.valueOf(c.getTag())) == radius) c.setChecked(true);
        }
    }

    private void startFresh() {
        // Never prompt for permission on open; if it is already granted, show nearby gyms right away.
        if (LocationHelper.hasPermission(this) && NetworkUtils.isOnline(this)) {
            findNearMe();
        } else {
            showIntro();
        }
    }

    private void restoreSession(Bundle s) {
        showingSaved = s.getInt(KEY_TAB, 0) == 1;
        TabLayout.Tab tab = b.tabs.getTabAt(showingSaved ? 1 : 0);
        if (tab != null) tab.select();
        int kind = s.getInt(KEY_KIND, KIND_NONE);
        String q = s.getString(KEY_QUERY, "");
        if (s.containsKey(KEY_REF_LAT)) {
            hasRef = true;
            refLat = s.getDouble(KEY_REF_LAT);
            refLng = s.getDouble(KEY_REF_LNG);
            refLabel = s.getString(KEY_REF_LABEL, "");
        }
        if (kind == KIND_NONE) {
            showIntro();
        } else if (kind == KIND_NEAR && !hasRef) {
            showIntro();
        } else {
            // Results are not stored in the bundle; the in-memory cache makes this a cheap repeat.
            lastQuery = q;
            runSearch(kind, q);
        }
    }

    // ------------------------------------------------------------------ tabs

    private void showTab(boolean saved) {
        showingSaved = saved;
        b.panelDiscover.setVisibility(saved ? View.GONE : View.VISIBLE);
        hideKeyboard();
        if (saved) {
            b.pbLoading.setVisibility(View.GONE);
            loadSaved();
        } else {
            if (discoverView != null) discoverView.run();
            else showIntro();
        }
    }

    private void updateTabCount() {
        TabLayout.Tab t = b.tabs.getTabAt(1);
        if (t != null) t.setText(savedIds.isEmpty() ? "Saved" : "Saved (" + savedIds.size() + ")");
    }

    // ------------------------------------------------------------------ search entry points

    private void onSearchPressed() {
        String q = String.valueOf(b.etSearch.getText()).trim();
        if (q.isEmpty()) {
            b.tilSearch.setError(b.chipModeLocation.isChecked() ? "Enter a place to search" : "Enter a gym name");
            return;
        }
        b.tilSearch.setError(null);
        hideKeyboard();
        runSearch(b.chipModeLocation.isChecked() ? KIND_AREA : KIND_NAME, q);
    }

    private void rerunLast() {
        if (lastKind == KIND_NEAR) findNearMe();
        else if (lastKind != KIND_NONE) runSearch(lastKind, lastQuery);
    }

    /** Device-location search. Asks for permission only here, and degrades gracefully when it is denied. */
    private void findNearMe() {
        lastKind = KIND_NEAR;
        lastQuery = "";
        permissionDeniedShown = false;
        // Every new search invalidates the previous one, including when it ends in an offline/permission screen.
        final int token = ++searchToken;
        if (!NetworkUtils.isOnline(this)) {
            showOffline();
            return;
        }
        if (!LocationHelper.hasPermission(this)) {
            if (!askedPermission) {
                askedPermission = true;
                permissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            } else {
                showPermissionDenied();
            }
            return;
        }
        setDiscoverLoading("Finding your location…");
        locationPendingToken = token;
        // Safety net: if the location service never answers, fall back to the "couldn't get your location" screen.
        b.getRoot().postDelayed(() -> {
            if (locationPendingToken == token && token == searchToken && !isFinishing() && !isDestroyed()) {
                locationPendingToken = -1;
                searchToken++;
                showLocationUnavailable();
            }
        }, LOCATION_TIMEOUT_MS);
        try {
            locationHelper.getLocation(loc -> {
                if (token != searchToken || isFinishing() || isDestroyed()) return;
                locationPendingToken = -1;
                if (loc == null) {
                    showLocationUnavailable();
                    return;
                }
                userLocation = loc;
                originIsDevice = true;
                hasRef = true;
                refLat = loc.getLatitude();
                refLng = loc.getLongitude();
                refLabel = "you";
                enableMyLocationLayer();
                fetchNearby(token);
            });
        } catch (RuntimeException e) {
            locationPendingToken = -1;
            showLocationUnavailable();
        }
    }

    private void onPermissionResult() {
        if (LocationHelper.hasPermission(this)) findNearMe();
        else showPermissionDenied();
    }

    /** Name or area search. */
    private void runSearch(int kind, String query) {
        lastKind = kind;
        lastQuery = query;
        final int token = ++searchToken;
        if (!NetworkUtils.isOnline(this)) {
            showOffline();
            return;
        }
        if (kind == KIND_NEAR) {
            findNearMe();
            return;
        }
        if (kind == KIND_AREA) {
            setDiscoverLoading("Searching " + query + "…");
            final int radius = radiusM;
            Async.network(() -> {
                OsmGymService.Place place = osm.geocode(query);
                List<Gym> gyms = osm.searchNearby(place.lat, place.lng, radius);
                return new AreaResult(place, gyms);
            }, (res, err) -> {
                if (token != searchToken || isFinishing() || isDestroyed()) return;
                if (err != null) {
                    showError(err);
                    return;
                }
                hasRef = true;
                refLat = res.place.lat;
                refLng = res.place.lng;
                refLabel = res.place.label;
                // Searching somewhere else: distances are measured from that place, not from the device.
                originIsDevice = false;
                applyResults(res.gyms, KIND_AREA, query);
            });
        } else {
            setDiscoverLoading("Searching for “" + query + "”…");
            final boolean bias = hasRef;
            final double lat = refLat;
            final double lng = refLng;
            Async.network(() -> osm.searchByName(query, bias, lat, lng), (list, err) -> {
                if (token != searchToken || isFinishing() || isDestroyed()) return;
                if (err != null) {
                    showError(err);
                    return;
                }
                applyResults(list, KIND_NAME, query);
            });
        }
    }

    private static final class AreaResult {
        final OsmGymService.Place place;
        final List<Gym> gyms;

        AreaResult(OsmGymService.Place place, List<Gym> gyms) {
            this.place = place;
            this.gyms = gyms;
        }
    }

    private void fetchNearby(final int token) {
        final double lat = refLat;
        final double lng = refLng;
        final int radius = radiusM;
        setDiscoverLoading("Looking for gyms around you…");
        Async.network(() -> osm.searchNearby(lat, lng, radius), (list, err) -> {
            if (token != searchToken || isFinishing() || isDestroyed()) return;
            if (err != null) {
                showError(err);
                return;
            }
            applyResults(list, KIND_NEAR, "");
        });
    }

    // ------------------------------------------------------------------ results

    private void applyResults(List<Gym> list, final int kind, final String query) {
        loading = false;
        discoverResults.clear();
        if (list != null) discoverResults.addAll(list);
        computeDistances(discoverResults);
        if (kind != KIND_NAME || hasRef) sortByDistance(discoverResults);
        while (discoverResults.size() > MAX_SHOWN) discoverResults.remove(discoverResults.size() - 1);

        final int n = discoverResults.size();
        final int radius = radiusM;
        final String label = refLabel;
        final String header;
        if (kind == KIND_NAME) {
            header = n + (n == 1 ? " result for “" : " results for “") + query + "”";
        } else if (kind == KIND_AREA) {
            header = n + (n == 1 ? " gym" : " gyms") + " within " + formatRadius(radius) + " of " + label;
        } else {
            header = n + (n == 1 ? " gym" : " gyms") + " within " + formatRadius(radius) + " of you";
        }

        setDiscoverView(() -> {
            b.pbLoading.setVisibility(View.GONE);
            loading = false;
            if (n == 0) {
                if (kind == KIND_NAME) {
                    showState(R.drawable.ic_search, "No gyms match “" + query + "”",
                            "Check the spelling, search an area instead, or look for gyms near you.",
                            "Find gyms near me", this::findNearMe, null, null);
                } else {
                    boolean canGrow = radius < 10000;
                    showState(R.drawable.ic_gym, "No gyms found",
                            "OpenStreetMap lists no gyms within " + formatRadius(radius)
                                    + (kind == KIND_AREA ? " of " + label : " of you") + "."
                                    + (canGrow ? " Try a larger radius." : " Try another area."),
                            canGrow ? "Try a larger radius" : "Search another area",
                            canGrow ? this::widenRadius : this::focusSearch, null, null);
                }
                showList(new ArrayList<Gym>(), null);
            } else {
                hideState();
                showList(discoverResults, header);
            }
            updateMarkers(discoverResults);
        });
    }

    private void widenRadius() {
        if (radiusM < 2000) selectRadiusChip(2000);
        else if (radiusM < 5000) selectRadiusChip(5000);
        else selectRadiusChip(10000);
    }

    private static String formatRadius(int m) {
        return m % 1000 == 0 ? (m / 1000) + " km" : m + " m";
    }

    /** Distance from the device when its position is known, otherwise from the searched place. */
    private void computeDistances(List<Gym> gyms) {
        double oLat;
        double oLng;
        if (originIsDevice && userLocation != null) {
            oLat = userLocation.getLatitude();
            oLng = userLocation.getLongitude();
        } else if (hasRef) {
            oLat = refLat;
            oLng = refLng;
        } else {
            for (Gym g : gyms) g.distanceMeters = -1f;
            return;
        }
        float[] out = new float[1];
        for (Gym g : gyms) {
            Location.distanceBetween(oLat, oLng, g.lat, g.lng, out);
            g.distanceMeters = out[0];
        }
    }

    private static void sortByDistance(List<Gym> gyms) {
        Collections.sort(gyms, new Comparator<Gym>() {
            @Override
            public int compare(Gym a, Gym c) {
                if (a.distanceMeters < 0 && c.distanceMeters < 0) return 0;
                if (a.distanceMeters < 0) return 1;
                if (c.distanceMeters < 0) return -1;
                return Float.compare(a.distanceMeters, c.distanceMeters);
            }
        });
    }

    // ------------------------------------------------------------------ saved tab

    private void loadSaved() {
        Async.run(() -> gymRepo.getSaved(), (saved, err) -> {
            if (!showingSaved || isFinishing() || isDestroyed()) return;
            savedGyms.clear();
            if (saved != null) {
                for (SavedGym s : saved) savedGyms.add(GymRepository.toGym(s));
            }
            renderSaved();
            // Distances for saved gyms: use a fix we already have, or fetch one if permission was granted before.
            if (userLocation == null && !savedGyms.isEmpty() && LocationHelper.hasPermission(this)) {
                try {
                    locationHelper.getLocation(loc -> {
                        if (loc == null || isFinishing() || isDestroyed()) return;
                        userLocation = loc;
                        if (showingSaved) renderSaved();
                    });
                } catch (RuntimeException ignored) {
                    // distances are optional
                }
            }
        });
    }

    private void renderSaved() {
        computeDistancesForSaved();
        b.pbLoading.setVisibility(View.GONE);
        if (savedGyms.isEmpty()) {
            showState(R.drawable.ic_bookmark, "No saved gyms yet",
                    "Tap Save on any gym in Discover and it will be kept here, even when you are offline.",
                    "Discover gyms", () -> {
                        TabLayout.Tab t = b.tabs.getTabAt(0);
                        if (t != null) t.select();
                    }, null, null);
            showList(new ArrayList<Gym>(), null);
        } else {
            hideState();
            int n = savedGyms.size();
            showList(savedGyms, n + (n == 1 ? " saved gym" : " saved gyms"));
        }
        updateMarkers(savedGyms);
    }

    private void computeDistancesForSaved() {
        if (userLocation != null) {
            float[] out = new float[1];
            for (Gym g : savedGyms) {
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), g.lat, g.lng, out);
                g.distanceMeters = out[0];
            }
            sortByDistance(savedGyms);
        } else {
            for (Gym g : savedGyms) g.distanceMeters = -1f;
        }
    }

    // ------------------------------------------------------------------ list actions

    @Override
    public void onOpen(Gym gym) {
        showDetails(gym);
    }

    @Override
    public void onDirections(Gym gym) {
        openDirections(gym);
    }

    @Override
    public void onToggleSave(final Gym gym) {
        final boolean wasSaved = savedIds.contains(gym.placeId);
        Async.run(() -> {
            if (wasSaved) gymRepo.remove(gym.placeId);
            else gymRepo.save(gym);
            return gymRepo.savedIds();
        }, (ids, err) -> {
            if (err != null || ids == null) {
                Toast.makeText(this, "Couldn't update saved gyms", Toast.LENGTH_SHORT).show();
                return;
            }
            savedIds = ids;
            updateTabCount();
            if (showingSaved) {
                if (wasSaved) {
                    for (int i = savedGyms.size() - 1; i >= 0; i--) {
                        if (savedGyms.get(i).placeId.equals(gym.placeId)) savedGyms.remove(i);
                    }
                    renderSaved();
                } else {
                    loadSaved();
                }
            } else {
                adapter.setSavedIds(savedIds);
            }
            Snackbar bar = Snackbar.make(b.getRoot(),
                    wasSaved ? "Removed " + gym.name : "Saved " + gym.name, Snackbar.LENGTH_LONG);
            if (wasSaved) {
                bar.setAction("Undo", v -> onToggleSave(gym));
            }
            bar.show();
        });
    }

    /** Hands navigation to whichever maps app the user picks. Only used by the Directions buttons. */
    private void openDirections(Gym gym) {
        Uri geo = Uri.parse("geo:" + gym.lat + "," + gym.lng + "?q=" + gym.lat + "," + gym.lng
                + "(" + Uri.encode(gym.name) + ")");
        Intent geoIntent = new Intent(Intent.ACTION_VIEW, geo);
        try {
            startActivity(Intent.createChooser(geoIntent, "Directions with…"));
        } catch (ActivityNotFoundException e1) {
            Uri web = Uri.parse("https://www.openstreetmap.org/?mlat=" + gym.lat + "&mlon=" + gym.lng
                    + "#map=17/" + gym.lat + "/" + gym.lng);
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, web));
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(this, "No maps app or browser available", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showDetails(final Gym gym) {
        DialogGymBinding d = DialogGymBinding.inflate(getLayoutInflater());
        d.tvGName.setText(gym.name);
        d.tvGCategory.setText(gym.category);
        d.tvGCategory.setVisibility(gym.category == null || gym.category.isEmpty() ? View.GONE : View.VISIBLE);
        d.tvGAddress.setText(GymAdapter.formatLocation(gym));
        String dist = GymAdapter.formatDistance(gym.distanceMeters);
        d.tvGMeta.setText(dist.isEmpty() ? "" : dist + " away");
        d.tvGMeta.setVisibility(dist.isEmpty() ? View.GONE : View.VISIBLE);

        if (gym.weekdayHours.isEmpty()) {
            d.tvGHours.setText("Opening hours aren't listed for this gym.");
        } else {
            StringBuilder hours = new StringBuilder("Opening hours");
            for (String line : gym.weekdayHours) hours.append("\n").append(line);
            d.tvGHours.setText(hours.toString());
        }
        StringBuilder contact = new StringBuilder();
        if (gym.phone != null && !gym.phone.isEmpty()) contact.append(gym.phone);
        if (gym.website != null && !gym.website.isEmpty()) {
            if (contact.length() > 0) contact.append("\n");
            contact.append(gym.website);
        }
        d.tvGContact.setText(contact.toString());
        d.tvGContact.setVisibility(contact.length() == 0 ? View.GONE : View.VISIBLE);
        d.btnGSave.setText(savedIds.contains(gym.placeId) ? "Unsave gym" : "Save gym");
        d.btnGSave.setIconResource(savedIds.contains(gym.placeId) ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);

        final AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(d.getRoot()).create();
        d.btnGDirections.setOnClickListener(v -> openDirections(gym));
        d.btnGSave.setOnClickListener(v -> {
            dialog.dismiss();
            onToggleSave(gym);
        });
        d.btnGWorkout.setOnClickListener(v -> {
            dialog.dismiss();
            Intent i = new Intent(this, WorkoutActivity.class);
            i.putExtra(WorkoutActivity.EXTRA_GYM_NAME, gym.name);
            i.putExtra(WorkoutActivity.EXTRA_GYM_ID, gym.placeId);
            startActivity(i);
        });
        dialog.show();
    }

    // ------------------------------------------------------------------ states (loading / empty / error)

    private void setDiscoverView(Runnable view) {
        discoverView = view;
        if (!showingSaved) view.run();
    }

    private void setDiscoverLoading(final String message) {
        loading = true;
        setDiscoverView(() -> {
            hideState();
            b.rvGyms.setVisibility(View.GONE);
            b.tvResultsHeader.setVisibility(View.VISIBLE);
            b.tvResultsHeader.setText(message);
            b.pbLoading.setVisibility(View.VISIBLE);
        });
    }

    private void showIntro() {
        loading = false;
        setDiscoverView(() -> {
            b.pbLoading.setVisibility(View.GONE);
            showState(R.drawable.ic_gym, "Find a gym",
                    "Search by gym name or place, or find gyms near you. Save your favourites to open them later, even offline.",
                    "Find gyms near me", this::findNearMe, null, null);
            showList(new ArrayList<Gym>(), null);
            updateMarkers(new ArrayList<Gym>());
        });
    }

    private void showOffline() {
        loading = false;
        setDiscoverView(() -> {
            b.pbLoading.setVisibility(View.GONE);
            showState(R.drawable.ic_alert, "You're offline",
                    "Searching for gyms needs an internet connection. Your saved gyms are still available.",
                    "Retry", this::rerunLast, "View saved gyms", this::openSavedTab);
            showList(new ArrayList<Gym>(), null);
        });
    }

    private void showPermissionDenied() {
        loading = false;
        permissionDeniedShown = true;
        setDiscoverView(() -> {
            b.pbLoading.setVisibility(View.GONE);
            showState(R.drawable.ic_my_location, "Location is off",
                    "Gym Finder works without your location: search by gym name or an area. "
                            + "Allow location to also find gyms right around you.",
                    "Search an area instead", this::searchAreaInstead, "Allow location", this::requestPermissionAgain);
            showList(new ArrayList<Gym>(), null);
        });
    }

    private void showLocationUnavailable() {
        loading = false;
        setDiscoverView(() -> {
            b.pbLoading.setVisibility(View.GONE);
            showState(R.drawable.ic_my_location, "Couldn't get your location",
                    "Turn on location services, or search an area or a gym name instead.",
                    "Try again", this::findNearMe, "Search an area instead", this::searchAreaInstead);
            showList(new ArrayList<Gym>(), null);
        });
    }

    private void showError(Exception e) {
        loading = false;
        String title;
        String msg = e.getMessage() == null ? "Something went wrong." : e.getMessage();
        if (e instanceof OsmGymService.OsmException) {
            switch (((OsmGymService.OsmException) e).kind) {
                case NETWORK: title = "Can't reach the server"; break;
                case TIMEOUT: title = "That took too long"; break;
                case RATE_LIMIT: title = "Server is busy"; break;
                case NOT_FOUND: title = "Place not found"; break;
                case SERVER: title = "Map server problem"; break;
                default: title = "Search failed"; break;
            }
        } else {
            title = "Search failed";
        }
        final String t = title;
        final String m = msg;
        final boolean notFound = e instanceof OsmGymService.OsmException
                && ((OsmGymService.OsmException) e).kind == OsmGymService.ErrorKind.NOT_FOUND;
        setDiscoverView(() -> {
            b.pbLoading.setVisibility(View.GONE);
            if (notFound) {
                showState(R.drawable.ic_search, t, m + " Check the spelling or try a bigger nearby town.",
                        "Edit search", this::focusSearch, "Find gyms near me", this::findNearMe);
            } else {
                showState(R.drawable.ic_alert, t, m, "Retry", this::rerunLast, "View saved gyms", this::openSavedTab);
            }
            showList(new ArrayList<Gym>(), null);
        });
    }

    private void requestPermissionAgain() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        } else {
            // "Don't ask again" (or first denial on some devices): only the system settings can change it.
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null)));
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, "Open Settings > Apps > LIFE RPG > Permissions", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void searchAreaInstead() {
        b.chipModeLocation.setChecked(true);
        focusSearch();
    }

    private void focusSearch() {
        b.etSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(b.etSearch, InputMethodManager.SHOW_IMPLICIT);
    }

    private void openSavedTab() {
        TabLayout.Tab t = b.tabs.getTabAt(1);
        if (t != null) t.select();
    }

    private void showList(List<Gym> gyms, String header) {
        if (header == null) {
            b.tvResultsHeader.setVisibility(View.GONE);
        } else {
            b.tvResultsHeader.setVisibility(View.VISIBLE);
            b.tvResultsHeader.setText(header);
        }
        b.rvGyms.setVisibility(gyms.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.submit(gyms, savedIds);
    }

    private void showState(int iconRes, String title, String message,
                           String primaryLabel, final Runnable primary,
                           String secondaryLabel, final Runnable secondary) {
        b.cardState.setVisibility(View.VISIBLE);
        b.ivState.setImageResource(iconRes);
        b.tvStateTitle.setText(title);
        b.tvStateMsg.setText(message);
        if (primaryLabel != null && primary != null) {
            b.btnStatePrimary.setVisibility(View.VISIBLE);
            b.btnStatePrimary.setText(primaryLabel);
            b.btnStatePrimary.setOnClickListener(v -> primary.run());
        } else {
            b.btnStatePrimary.setVisibility(View.GONE);
        }
        if (secondaryLabel != null && secondary != null) {
            b.btnStateSecondary.setVisibility(View.VISIBLE);
            b.btnStateSecondary.setText(secondaryLabel);
            b.btnStateSecondary.setOnClickListener(v -> secondary.run());
        } else {
            b.btnStateSecondary.setVisibility(View.GONE);
        }
    }

    private void hideState() {
        b.cardState.setVisibility(View.GONE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(b.etSearch.getWindowToken(), 0);
    }

    // ------------------------------------------------------------------ optional Google map

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        if (night) {
            try {
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
            } catch (Exception ignored) {
                // style is cosmetic only
            }
        }
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Gym) showDetails((Gym) tag);
        });
        enableMyLocationLayer();
        updateMarkers(showingSaved ? savedGyms : discoverResults);
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocationLayer() {
        if (map != null && LocationHelper.hasPermission(this)) map.setMyLocationEnabled(true);
    }

    private void updateMarkers(List<Gym> gyms) {
        if (map == null) return;
        map.clear();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        LatLng first = null;
        int count = 0;
        for (Gym g : gyms) {
            LatLng p = new LatLng(g.lat, g.lng);
            Marker m = map.addMarker(new MarkerOptions().position(p).title(g.name).snippet(g.address));
            if (m != null) m.setTag(g);
            bounds.include(p);
            if (first == null) first = p;
            count++;
        }
        if (hasRef && !showingSaved && !gyms.isEmpty()) {
            LatLng ref = new LatLng(refLat, refLng);
            map.addCircle(new CircleOptions().center(ref).radius(radiusM)
                    .strokeColor(0xAA5E9E6E).fillColor(0x145E9E6E).strokeWidth(3f));
            bounds.include(ref);
            count++;
        }
        if (count == 0 || first == null) return;
        try {
            int w = b.mapView.getWidth();
            int h = b.mapView.getHeight();
            if (count > 1 && w > 0 && h > 0) {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), w, h, 90));
            } else {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(first, 14f));
            }
        } catch (Exception e) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 13f));
        }
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onStart() {
        super.onStart();
        if (mapEnabled) b.mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapEnabled) b.mapView.onResume();
        // Coming back from system settings after granting location: pick it up without another tap.
        if (permissionDeniedShown && !showingSaved && LocationHelper.hasPermission(this)) {
            permissionDeniedShown = false;
            findNearMe();
        }
    }

    @Override
    protected void onPause() {
        if (mapEnabled) b.mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mapEnabled) b.mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        searchToken++; // late network answers are ignored
        if (mapEnabled) b.mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapEnabled) b.mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapEnabled) {
            Bundle mapState = new Bundle();
            b.mapView.onSaveInstanceState(mapState);
            outState.putBundle(KEY_MAP, mapState);
        }
        outState.putInt(KEY_TAB, showingSaved ? 1 : 0);
        outState.putInt(KEY_KIND, lastKind);
        outState.putString(KEY_QUERY, lastQuery);
        outState.putBoolean(KEY_MODE_AREA, b.chipModeLocation.isChecked());
        outState.putInt(KEY_RADIUS, radiusM);
        if (hasRef) {
            outState.putDouble(KEY_REF_LAT, refLat);
            outState.putDouble(KEY_REF_LNG, refLng);
            outState.putString(KEY_REF_LABEL, refLabel);
        }
    }
}
