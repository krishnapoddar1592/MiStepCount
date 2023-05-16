package com.example.mistepcount;

import android.Manifest;


import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class StepServiceLocation extends Service implements SensorEventListener {
    int brand=-1;
    public static final int SPEED_THRESHOLD=13;
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
    private TensorFlowClassifier classifier;
    /**
     * Native Library Object
     */
    private NativeLibraryLoc nativeLib;
    private NativeLibraryModel nativeModel;
    FusedLocationProviderClient fusedLocationProviderClient;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        classifier = new TensorFlowClassifier(getApplicationContext());
        HashMap<String,Integer> brands =new HashMap<>();
        brands.put("redmi",0);
        brands.put("realme",1);
        brands.put("oneplus",2);
        brands.put("samsung",3);

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
        nativeModel=new NativeLibraryModel();
        handler = new Handler();
        final int delay = 20000; // 1000 milliseconds == 1 second
        handler.postDelayed(new Runnable() {

            public void run() {
//                Toast.makeText(getApplicationContext(), "location", Toast.LENGTH_SHORT).show();
                try {
                    sum = sum + onPassDataToJNIButtonClicked();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
    public ArrayList<ArrayList<ArrayList<Float>>> createSegments(List<List<Double>> acc, int timeSteps) {
        final int N_FEATURES = 3;
        ArrayList<ArrayList<ArrayList<Float>>> segments = new ArrayList<>();
        for (int i = 0; i < acc.size()-timeSteps ; i += timeSteps) {
            ArrayList<ArrayList<Float>> segment = new ArrayList<>(N_FEATURES);
            segment.add(new ArrayList<Float>(timeSteps));
            segment.add(new ArrayList<Float>(timeSteps));
            segment.add(new ArrayList<Float>(timeSteps));
            for (int j = 0; j < timeSteps; j++) {
//                System.out.println(Arrays.toString(acc.get(i+j).toArray()));
                segment.get(0).add((acc.get(i + j).get(0).floatValue()));
                segment.get(1).add((acc.get(i + j).get(1).floatValue()));
                segment.get(2).add((acc.get(i + j).get(2).floatValue()));
            }
            segments.add(segment);
        }
        return segments;
    }
    public static void print2DArray(double[][] array) {
        for(int i=0; i<array.length; i++) {
            for(int j=0; j<array[i].length; j++) {
                System.out.print(array[i][j] + " ");
            }
            System.out.println();
        }
    }

    public double[][] createSegmentsArray(List<List<Double>> acc) {
        final int N_FEATURES = 10;
        ArrayList<ArrayList<Double>> segments = new ArrayList<>();
        for (int i = 0; i < acc.size() - N_FEATURES-1; i += N_FEATURES) {
            ArrayList<Double> segment = new ArrayList<>();
            for (int j = 0; j < N_FEATURES; j++) {
                float ax = acc.get(i + j).get(0).floatValue();
                float ay = acc.get(i + j).get(1).floatValue();
                float az = acc.get(i + j).get(2).floatValue();
                double rms = Math.pow((Math.pow(ax, 2) + Math.pow(ay, 2) + Math.pow(az, 2)) / 3, 0.5);
                segment.add(rms);
            }
            segments.add(segment);
        }
        double[][] segmentsArray = new double[segments.size()][N_FEATURES];
        for (int i = 0; i < segments.size(); i++) {
            for (int j = 0; j < N_FEATURES; j++) {
                segmentsArray[i][j] = segments.get(i).get(j);
            }
        }
        return segmentsArray;
    }



    public static int argmax(float[] array) {
        int index = 0;
        float max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                index = i;
                max = array[i];
            }
        }
        return index;
    }

    public static int maxProb(int[] indeces){
        Map<Integer, Integer> map = new HashMap<>();
        for(int i = 0; i < indeces.length; i++) {
            if(map.containsKey(indeces[i])) {
                map.put(indeces[i], map.get(indeces[i])+1);
            }
            else {
                map.put(indeces[i], 1);
            }
        }
        int max = Integer.MIN_VALUE;
        int maxIndex = -1;
        for(Integer key: map.keySet()) {
            if(map.get(key) > max) {
                max = map.get(key);
                maxIndex = key;
            }
        }
        return maxIndex;
    }



    public int onPassDataToJNIButtonClicked() throws IOException {
        // Temp data which passing to JNI
        String[] labels={"Downstairs", "Jogging", "Sitting", "Standing", "Upstairs", "Walking"};
        rmsSize=rms.size();
        AccSize+=rmsSize;
        ArrayList<ArrayList<ArrayList<Float>>> x_new=createSegments(acc,200);
        double[][] rmsNew=createSegmentsArray(acc);
        print2DArray(rmsNew);
        if (x_new.size()==0){
            double[] tmpArray = new double[rms.size()];
            for (int i = 0; i < tmpArray.length; i++) {
                tmpArray[i] = rms.get(i);
            }
            rms.clear();
            acc.clear();
            return nativeLib.passingDataToJni(tmpArray,tmpArray.length,brand);
        }
//        Toast.makeText(getApplicationContext(), String.valueOf(rmsSize), Toast.LENGTH_SHORT).show();

//        rms.clear();
        acc.clear();

        ArrayList<Double> tempRms=new ArrayList<Double>();

        if(rmsSize>0) {
            Toast.makeText(this, String.valueOf(x_new.size()), Toast.LENGTH_SHORT).show();
            int[] y_classes=new int[x_new.size()];
            int[] y_classes2=new int[rmsNew.length];
            for(int i =0; i<rmsNew.length;i++){
                System.out.println(Arrays.toString(rmsNew[i])
                );
                y_classes2[i]=nativeModel.passingDataToJni(rmsNew[i],1,2);
            }

            for(int i =0;i<x_new.size();i++){
                List<Float> data = new ArrayList<>();
                data.addAll(x_new.get(i).get(0));
                data.addAll(x_new.get(i).get(1));
                data.addAll(x_new.get(i).get(2));
                float [] results = classifier.predictProbabilities(toFloatArray(data));
                y_classes[i]=argmax(results);
            }
            Toast.makeText(this, Arrays.toString(y_classes2), Toast.LENGTH_SHORT).show();
            System.out.println("Rms: "+Arrays.toString(tempRms.toArray()));
//            System.out.println("Prediction: "+Arrays.toString(y_classes));
//            System.out.println("Prediction: "+Arrays.toString(y_classes2));
//                System.out.println(labels[maxProb(y_classes)]);
            Toast.makeText(getApplicationContext(),labels[maxProb(y_classes)],Toast.LENGTH_SHORT).show();
            if(maxProb(y_classes)!=2 && maxProb(y_classes)!=3) {
                double[] tmpArray = new double[rms.size()];
                for (int i = 0; i < tmpArray.length; i++) {
                    tmpArray[i] = rms.get(i);
                }
                rms.clear();
                tempRms.clear();
                int tmpInt = tmpArray.length;


                return nativeLib.passingDataToJni(tmpArray, tmpInt, brand);
            }
            else{
                return 0;
            }
        }
        else{
            return 0;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
        handler.removeCallbacksAndMessages(null);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
         SharedPreferences locSp= getApplicationContext().getSharedPreferences("com.example.mistepcount.locationservice", Context.MODE_PRIVATE);
        double speed=locSp.getFloat("speed",0);

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
    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
