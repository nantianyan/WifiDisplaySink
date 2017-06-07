package com.lc.wifidisplaysink;

import java.io.IOException;
import android.app.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Binder;
import android.os.IBinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.view.WindowManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.lang.Math;
import android.util.Log;

import android.app.Service;

public class HidDeviceAdapterService extends Service{
    private static final String TAG = "HidDeviceAdapterService";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

     //Because we know this service always
     //runs in the same process as its clients, we don't need to deal with
     //IPC.
    public class HidDeviceAdapterBinder extends Binder {
        HidDeviceAdapterService getService() {
            Log.d(TAG, "getService()");
            return HidDeviceAdapterService.this;
        }
    }

    private final IBinder mBinder = new HidDeviceAdapterBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return mBinder;
    }

    //----------------------------------------------------
     public void sendLeftClickReport() {
         Log.d(TAG, "sendLeftClickReport");
     }

     public void sendMouseMoveReport(int delta_x, int delta_y) {
         Log.d(TAG, "sendMouseMoveReport");
     }

}
