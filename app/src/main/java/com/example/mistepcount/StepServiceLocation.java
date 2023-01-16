package com.example.mistepcount;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class StepServiceLocation extends Service implements SensorEventListener {
    int brand=-1;
    public static final int SPEED_THRESHOLD=10;
    private Location mLocation;
    public Location previousLocation;
    public int sum = 0;
    public double mean = 0.0;
    public Instant start = Instant.now();
    public List<List<Double>> acc = new ArrayList<>();
    public List<Double> rms = new ArrayList<>();
    public List<Double> x = new ArrayList<>();
    public List<Double> y = new ArrayList<>();
    public List<Double> z = new ArrayList<>();
    public List<Double> second = new ArrayList<>();
    private LocationCallback locationCallback;
    public Handler handler;
    Sensor accelerometer;
    int rmsSize = 0;
    int AccSize = 0;


    /**
     * Native Library Object
     */
    private NativeLibraryLoc nativeLib;
    FusedLocationProviderClient fusedLocationProviderClient;



    private static Double distance(Location one, Location two) {
        int R = 6371000;
        Double dLat = toRad(two.getLatitude() - one.getLatitude());
        Double dLon = toRad(two.getLongitude() - one.getLongitude());
        Double lat1 = toRad(one.getLatitude());
        Double lat2 = toRad(two.getLatitude());
        Double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        Double d = R * c;
        return d;
    }

    private static double toRad(Double d) {
        return d * Math.PI / 180;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        HashMap<String,Integer> brands =new HashMap<>();
        brands.put("Redmi",0);
        brands.put("realme",1);
        brands.put("Oneplus",2);
        brands.put("Samsung",3);

        String brandName = android.os.Build.BRAND;
        brandName=brandName.toLowerCase(Locale.ROOT);
        Toast.makeText(getApplicationContext(), brandName, Toast.LENGTH_SHORT).show();
        int x = -1;
        if(brands.containsKey(brandName)){
            x = brands.get(brandName);
        }
        brand =x;
        SharedPreferences sp = getApplicationContext().getSharedPreferences("com.example.mistepcount.stepservicelocation", Context.MODE_PRIVATE);
        try {
            sum = Integer.parseInt(sp.getString("sum", "0"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener((SensorEventListener) StepServiceLocation.this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        nativeLib = new NativeLibraryLoc();
        handler = new Handler();
        final int delay = 20000; // 1000 milliseconds == 1 second
        handler.postDelayed(new Runnable() {

            public void run() {
//                Toast.makeText(getApplicationContext(), "location", Toast.LENGTH_SHORT).show();
                sum = sum + onPassDataToJNIButtonClicked();
                //                sum=onPassDataToJNIButtonClicked();

                try {
                    sp.edit().clear().apply();
                    sp.edit().putString("sum", String.valueOf(sum)).apply();
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                System.out.println(rms.toString());
                int count = 0;
                handler.postDelayed(this, delay);
            }
        }, delay);
        return START_STICKY;
    }

    public int onPassDataToJNIButtonClicked() {
        // Temp data which passing to JNI
        rmsSize=rms.size();
        AccSize+=rmsSize;
        System.out.println("reaches:" +rmsSize);
        Toast.makeText(getApplicationContext(), String.valueOf(rmsSize), Toast.LENGTH_SHORT).show();
        double[] tmpArray =new double[rmsSize];
        for(int i =0; i<rmsSize ;i++){
            tmpArray[i]=rms.get(i);
        }
        rms.clear();
//        acc.clear();
        int tmpInt = tmpArray.length;


        // Pass data & get error code
        //        System.out.println("Success"+returnValue);
        if(rmsSize>0) {
            return nativeLib.passingDataToJni(tmpArray, tmpInt, brand);
        }
        else{
            return 0;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
         SharedPreferences locSp= getApplicationContext().getSharedPreferences("com.example.mistepcount.locationservice", Context.MODE_PRIVATE);
        double speed=locSp.getFloat("speed",0);

//        System.out.println(speed);
//        Toast.makeText(getApplicationContext(), String.valueOf(speed), Toast.LENGTH_SHORT).show();
        if (speed<=SPEED_THRESHOLD) {
            Instant current =Instant.now();
            final DecimalFormat df = new DecimalFormat("0.0");
            List<Double> tempList =new ArrayList<>();
            double ax=(double) event.values[0];
            double ay=(double) event.values[1];
            double az=(double) event.values[2];
            tempList.add(ax);
            x.add(ax);
            y.add(ay);
            z.add(az);
            second.add((double) Duration.between(start, current).toMillis() / 1000.0);
            tempList.add(ay);
            tempList.add(az);
            acc.add(tempList);
            double rmsValue=Math.pow((Math.pow(ax,2)+Math.pow(ay,2)+Math.pow(az,2))/3,0.5);

            rms.add(Double.parseDouble(df.format(rmsValue)));
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
