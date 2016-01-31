package com.jallier.kitchentimer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class svTimerService extends Service {
    public static boolean isServiceRunning = false;

    private final String LOGTAG = getClass().getSimpleName();
    private final IBinder myBinder = new MyBinder();

    private Stopwatch stopwatch;

    public svTimerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    public class MyBinder extends Binder {
        svTimerService getService() {
            return svTimerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOGTAG, "Service created");
        stopwatch = new Stopwatch();
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        Log.d(getClass().getSimpleName(), "Service stopped");
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOGTAG, "Service started");
        isServiceRunning = true;
        stopwatch.run();

        //Handler fires off a broadcast intent every 1000ms, which is received in the activity and updates the textview
        final Handler h = new Handler();
        final int delay = 1000; //milliseconds
        h.postDelayed(new Runnable() {
            public void run() {
                Log.d(LOGTAG, "Handler runs");
                sendBroadcast(stopwatch.getStringElapsedTime());
                h.postDelayed(this, delay);
            }
        }, delay);
        return START_NOT_STICKY;
    }

    private void sendBroadcast(String timerValue) {
        Intent intent = new Intent("timerIntent");
        intent.putExtra("value", timerValue);
        sendBroadcast(intent);
    }

    public Stopwatch.TimerState getTimerState() {
        return stopwatch.getState();
    }

    public void startTimer() {
        Log.d(LOGTAG, "Service started");
        isServiceRunning = true;
        stopwatch.run();

        //Handler fires off a broadcast intent every 1000ms, which is received in the activity and updates the textview
        final Handler h = new Handler();
        final int delay = 1000; //milliseconds

        h.postDelayed(new Runnable() {
            public void run() {
                Log.d(LOGTAG, "Handler runs");
                sendBroadcast(stopwatch.getStringElapsedTime());
                h.postDelayed(this, delay);
            }
        }, delay);
    }

    public void restartTimer() {
        stopwatch.run();
    }

    public void pauseTimer() {
        //Need to make this cleaner
        stopwatch.run();
    }
}
