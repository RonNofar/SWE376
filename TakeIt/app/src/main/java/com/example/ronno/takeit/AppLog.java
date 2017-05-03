package com.example.ronno.takeit;

import android.util.Log;

/**
 * Created by ronno on 4/17/2017.
 */

public class AppLog {

    private static final String APP_TAG = "AudioRecorder";

    public static int logString(String message) {
        return Log.i(APP_TAG, message);
    }
}
