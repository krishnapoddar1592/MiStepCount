package com.example.mistepcount;

public class NativeLibrary {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native int passingDataToJni(double[] tmpArray, int tmpInt, double tmpDouble);


}
