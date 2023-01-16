package com.example.mistepcount;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.LocationCallback;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    public int sum =0;
    final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final boolean[] useLocation = {false};

        TextView textView=findViewById(R.id.textView);
        Button button =findViewById(R.id.reset);
        Button locationButton =findViewById(R.id.button);
        locationButton.setText("Enable Location");
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            useLocation[0]=false;
//            locationButton.setText("Enable Location");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        }


//
        if(!isMyServiceRunning(StepService.class) && !isMyServiceRunning(StepServiceLocation.class)){
            if (useLocation[0]) {
                locationButton.setText("Disable Location");
                startService(new Intent(MainActivity.this,StepServiceLocation.class));
                startService(new Intent(MainActivity.this, LocationService.class));

            }
            else{
                locationButton.setText("Enable Location");
                startService(new Intent(MainActivity.this,StepService.class));
            }

        }
        final SharedPreferences[] sp = new SharedPreferences[1];
        final SharedPreferences[] loc = new SharedPreferences[1];
        loc[0]=getApplicationContext().getSharedPreferences("com.example.mistepcount.locationservice",Context.MODE_PRIVATE);
        if(useLocation[0]) {
            sp[0] = getApplicationContext().getSharedPreferences("com.example.mistepcount.stepservicelocation", Context.MODE_PRIVATE);
        }
        else{
            sp[0] = getApplicationContext().getSharedPreferences("com.example.mistepcount.stepservice", Context.MODE_PRIVATE);
        }

        int newSum=0;
        try {
            newSum= Integer.parseInt(sp[0].getString("sum","0"));
            System.out.println(newSum+"gergegerg");
            sum=newSum;
        } catch (Exception e) {
            e.printStackTrace();
        }
        textView.setText("Steps: "+sum);

        final Handler handler = new Handler();
        final int delay = 21000; // 1000 milliseconds == 1 second

        handler.postDelayed(new Runnable() {
            public void run() {
//                Toast.makeText(MainActivity.this,"handler original",Toast.LENGTH_SHORT).show();
                try {
                    sum= Integer.parseInt(sp[0].getString("sum","0"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                textView.setText("Steps: "+ sum);
                handler.postDelayed(this, delay);
            }
        }, delay);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!useLocation[0]) {

                    useLocation[0] = true;
                    locationButton.setText("Disable location");
                    try {
                        // Stop and restart the StepService service
//                    Toast.makeText(MainActivity.this,"resetting the steps to 0",Toast.LENGTH_SHORT).show();
                        Intent stopIntent = new Intent(MainActivity.this, StepService.class);
                        stopService(stopIntent);
                        startService(new Intent(MainActivity.this, LocationService.class));
                        startService(new Intent(MainActivity.this, StepServiceLocation.class));

                        sp[0] = getApplicationContext().getSharedPreferences("com.example.mistepcount.stepservicelocation", Context.MODE_PRIVATE);
                        // Reset the SharedPreferences object
                        sp[0].edit().clear().apply();
                        loc[0].edit().clear().apply();
                        try {
                            sum = Integer.parseInt(sp[0].getString("sum", "0"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                    Toast.makeText(MainActivity.this,"reset the steps to"+sum,Toast.LENGTH_SHORT).show();
                        textView.setText("Steps: 0");

                        // Reset the Handler
                        handler.removeCallbacksAndMessages(null);
                        handler.postDelayed(new Runnable() {
                            public void run() {
//                            Toast.makeText(MainActivity.this,"handler after resetting",Toast.LENGTH_SHORT).show();
                                try {
                                    sum = Integer.parseInt(sp[0].getString("sum", "0"));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                textView.setText("Steps: " + sum);
                                handler.postDelayed(this, delay);
                            }
                        }, delay);
                    } catch (Exception e) {

                    }

                }
                else{
                    useLocation[0]=false;
                    locationButton.setText("Enable location");
                    try {
                        // Stop and restart the StepService service
//                    Toast.makeText(MainActivity.this,"resetting the steps to 0",Toast.LENGTH_SHORT).show();
                        Intent stopIntent = new Intent(MainActivity.this, StepServiceLocation.class);
                        stopService(stopIntent);
                        Intent stopIntent2 = new Intent(MainActivity.this, LocationService.class);
                        stopService(stopIntent2);
                        startService(new Intent(MainActivity.this, StepService.class));
                        sp[0] = getApplicationContext().getSharedPreferences("com.example.mistepcount.stepservice", Context.MODE_PRIVATE);
                        // Reset the SharedPreferences object
                        sp[0].edit().clear().apply();
                        loc[0].edit().clear().apply();
                        try {
                            sum = Integer.parseInt(sp[0].getString("sum", "0"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                    Toast.makeText(MainActivity.this,"reset the steps to"+sum,Toast.LENGTH_SHORT).show();
                        textView.setText("Steps: 0");

                        // Reset the Handler
                        handler.removeCallbacksAndMessages(null);
                        handler.postDelayed(new Runnable() {
                            public void run() {
//                            Toast.makeText(MainActivity.this,"handler after resetting",Toast.LENGTH_SHORT).show();
                                try {
                                    sum = Integer.parseInt(sp[0].getString("sum", "0"));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                textView.setText("Steps: " + sum);
                                handler.postDelayed(this, delay);
                            }
                        }, delay);
                    } catch (Exception e) {

                    }
                }

            }
        });
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                if(!useLocation[0]) {
                    try {
                        // Stop and restart the StepService service
//                    Toast.makeText(MainActivity.this,"resetting the steps to 0",Toast.LENGTH_SHORT).show();
                        Intent stopIntent = new Intent(MainActivity.this, StepService.class);
                        stopService(stopIntent);
                        startService(new Intent(MainActivity.this, StepService.class));

                        // Reset the SharedPreferences object
                        sp[0].edit().clear().apply();
                        try {
                            sum = Integer.parseInt(sp[0].getString("sum", "0"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                    Toast.makeText(MainActivity.this,"reset the steps to"+sum,Toast.LENGTH_SHORT).show();
                        textView.setText("Steps: 0");

                        // Reset the Handler
                        handler.removeCallbacksAndMessages(null);
                        handler.postDelayed(new Runnable() {
                            public void run() {
//                            Toast.makeText(MainActivity.this,"handler after resetting",Toast.LENGTH_SHORT).show();
                                try {
                                    sum = Integer.parseInt(sp[0].getString("sum", "0"));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                textView.setText("Steps: " + sum);
                                handler.postDelayed(this, delay);
                            }
                        }, delay);
                    } catch (Exception e) {

                    }
                }
                else{
                    try {
                        // Stop and restart the StepService service
//                    Toast.makeText(MainActivity.this,"resetting the steps to 0",Toast.LENGTH_SHORT).show();
                        Intent stopIntent2 = new Intent(MainActivity.this, LocationService.class);
                        stopService(stopIntent2);
                        startService(new Intent(MainActivity.this, LocationService.class));
                        Intent stopIntent = new Intent(MainActivity.this, StepServiceLocation.class);
                        stopService(stopIntent);
                        startService(new Intent(MainActivity.this, StepServiceLocation.class));

                        // Reset the SharedPreferences object

                        sp[0].edit().clear().apply();
                        loc[0].edit().clear().apply();
                        try {
                            sum = Integer.parseInt(sp[0].getString("sum", "0"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                    Toast.makeText(MainActivity.this,"reset the steps to"+sum,Toast.LENGTH_SHORT).show();
                        textView.setText("Steps: 0");

                        // Reset the Handler
                        handler.removeCallbacksAndMessages(null);
                        handler.postDelayed(new Runnable() {
                            public void run() {
//                            Toast.makeText(MainActivity.this,"handler after resetting",Toast.LENGTH_SHORT).show();
                                try {
                                    sum = Integer.parseInt(sp[0].getString("sum", "0"));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                textView.setText("Steps: " + sum);
                                handler.postDelayed(this, delay);
                            }
                        }, delay);
                    } catch (Exception e) {

                    }
                }

            }
        });

    }
}