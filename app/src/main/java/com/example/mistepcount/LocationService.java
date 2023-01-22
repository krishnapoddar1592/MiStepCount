package com.example.mistepcount;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class LocationService extends Service {
    private Location mLocation;
    private Location previousLocation;
    private double speed;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getApplicationContext().getSharedPreferences("com.example.mistepcount.locationservice", Context.MODE_PRIVATE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        mLocation = location;
                        calculateSpeed();
                        storeSpeed();
                    }
                }
            }
        };
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1500); //5 seconds
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void calculateSpeed() {
        if (previousLocation != null) {
            double distance = mLocation.distanceTo(previousLocation); // distance in meters
            long timeDiff = (mLocation.getTime() - previousLocation.getTime()) / 1000; // time difference in seconds
            speed = (distance / timeDiff)*3.6; // speed in kmph
        }
        previousLocation = mLocation;
    }

    private void storeSpeed() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
//        Toast.makeText(getApplicationContext(), String.valueOf(speed), Toast.LENGTH_SHORT).show();
        editor.putFloat("speed", (float) speed);
        editor.apply();
    }
}

