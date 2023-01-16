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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StepService extends Service implements SensorEventListener {
    public int sum = 0;
    public double mean = 0.0;
    public Instant start = Instant.now();
    public List<List<Double>> acc = new ArrayList<>();
    public List<Double> rms = new ArrayList<>();
    public List<Double> x = new ArrayList<>();
    public List<Double> y = new ArrayList<>();
    public List<Double> z = new ArrayList<>();
    public List<Double> second = new ArrayList<>();
    public Handler handler;
    Sensor accelerometer;
    int rmsSize = 0;
    int AccSize = 0;
    /**
     * Native Library Object
     */
    private NativeLibrary nativeLib;
    FusedLocationProviderClient fusedLocationProviderClient;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sp = getApplicationContext().getSharedPreferences("com.example.mistepcount.stepservice", Context.MODE_PRIVATE);
        try {
            sum = Integer.parseInt(sp.getString("sum", "0"));
        } catch (Exception e) {
            e.printStackTrace();
        }


        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener((SensorEventListener) StepService.this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        nativeLib = new NativeLibrary();
        handler = new Handler();
        final int delay = 20000; // 1000 milliseconds == 1 second
        handler.postDelayed(new Runnable() {
            public void run() {
//                Toast.makeText(getApplicationContext(), "normal", Toast.LENGTH_SHORT).show();
                sum = sum + onPassDataToJNIButtonClicked();
//                sum=onPassDataToJNIButtonClicked();

                try {
                    sp.edit().clear().apply();
                    sp.edit().putString("sum", String.valueOf(sum)).apply();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println(rms.toString());
                int count = 0;
//                if(acc.size()>=4900){
//                    System.out.println("dfefrefwerfwerferfer");
//                    Toast.makeText(getApplicationContext(), "Saving file", Toast.LENGTH_SHORT).show();
//                            // Data to save in the CSV file
//                        double[] sec = second.stream().mapToDouble(Double::doubleValue).toArray();
//                        double[] az = z.stream().mapToDouble(Double::doubleValue).toArray();
//                        double[] ax = x.stream().mapToDouble(Double::doubleValue).toArray();
//                        double[] ay = y.stream().mapToDouble(Double::doubleValue).toArray();
//
//                        // Create a StringBuilder to hold the data
//                        StringBuilder sb = new StringBuilder();
//                        sb.append("time").append(",").append("seconds_elapsed").append(",").append("z").append(",").append("y").append(",").append("x").append("\n");
//                        // Iterate over the arrays and append the data to the StringBuilder in the correct format
//                        for (int i = 0; i < sec.length; i++) {
//                            sb.append(Long.parseLong("1667880000000000000")).append(",").append(sec[i]).append(",").append(az[i]).append(",").append(ay[i]).append(",").append(ax[i]).append("\n");
//                        }
//
//                        // Convert the StringBuilder to a String
//                        String data = sb.toString();
//
//                        // Write the String to a file usig a BufferedWriter
//                    File file = new File(getExternalFilesDir(null), "Accelerometer.csv");
//
//                    try {
//                        // Write the CSV data to the file
//                        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
//                        writer.write(data);
//                        writer.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    Toast.makeText(getApplicationContext(), "file saved", Toast.LENGTH_SHORT).show();
//                }
//                System.out.println(Arrays.toString(rms.toArray()));
                handler.postDelayed(this, delay);
            }
        }, delay);
        return START_STICKY;
    }


    public int onPassDataToJNIButtonClicked() {
        // Temp data which passing to JNI
        rmsSize=rms.size();
        AccSize+=rmsSize;
        Toast.makeText(getApplicationContext(), String.valueOf(rmsSize), Toast.LENGTH_SHORT).show();
        System.out.println("reaches:" +rmsSize);

        double[] tmpArray =new double[rmsSize];
        for(int i =0; i<rmsSize ;i++){
            tmpArray[i]=rms.get(i);
        }
        rms.clear();
//        acc.clear();
        int tmpInt = tmpArray.length;
        double tmpDouble=mean;

        // Pass data & get error code
        //        System.out.println("Success"+returnValue);
        return nativeLib.passingDataToJni(tmpArray, tmpInt, tmpDouble);
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
