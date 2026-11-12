package com.example.liferpg.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

/** Thin wrapper around the Fused Location Provider. */
public class LocationHelper {

    public interface Callback {
        /** Called on the main thread. location is null if it could not be determined. */
        void onLocation(@Nullable Location location);
    }

    private final Context context;
    private final FusedLocationProviderClient client;

    public LocationHelper(Context context) {
        this.context = context;
        this.client = LocationServices.getFusedLocationProviderClient(context);
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    public void getLocation(final Callback callback) {
        if (!hasPermission(context)) {
            callback.onLocation(null);
            return;
        }
        CancellationTokenSource cts = new CancellationTokenSource();
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) callback.onLocation(location);
                    else fallbackToLastLocation(callback);
                })
                .addOnFailureListener(e -> fallbackToLastLocation(callback));
    }

    @SuppressLint("MissingPermission")
    private void fallbackToLastLocation(final Callback callback) {
        if (!hasPermission(context)) {
            callback.onLocation(null);
            return;
        }
        client.getLastLocation()
                .addOnSuccessListener(location -> callback.onLocation(location))
                .addOnFailureListener(e -> callback.onLocation(null));
    }
}
