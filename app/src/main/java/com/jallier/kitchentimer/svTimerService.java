package com.jallier.kitchentimer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Random;

public class svTimerService extends Service {
    private final String LOGTAG = getClass().getSimpleName();
    private final String INTENT_FILTER_TIMERS = "com.jallier.kitchentimer" + ".timers";
    private final String INTENT_EXTRA_TIMER1 = "com.jallier.kitchentimer" + ".timer1";
    private final String INTENT_EXTRA_TIMER2 = "com.jallier.kitchentimer" + ".timer2";
    private final String INTENT_EXTRA_TIMER3 = "com.jallier.kitchentimer" + ".timer3";
    private final String INTENT_EXTRA_TIMER4 = "com.jallier.kitchentimer" + ".timer4";
    private final IBinder myBinder = new MyBinder();

    private Stopwatch stopwatch;
    private Handler h;

    public svTimerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        //startHandler();
        Log.d(LOGTAG, "service binds");
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
        stopHandler();
        Log.d(getClass().getSimpleName(), "Service stopped");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateTimer();
        return super.onStartCommand(intent, flags, startId);
    }

    private void sendBroadcast(String timerValue) {
        Intent intent = new Intent(INTENT_FILTER_TIMERS);
        intent.putExtra(INTENT_EXTRA_TIMER1, timerValue);
        sendBroadcast(intent);
    }

    public Stopwatch.TimerState getTimerState() {
        return stopwatch.getState();
    }

    public void startTimer() {
        Log.d(LOGTAG, "Service started");
        stopwatch.run();

        startHandler();
    }

    private void startHandler() {
        //Handler fires off a broadcast intent every 1000ms, which is received in the activity and updates the textview
        h = new Handler();
        final int delay = 1000; //milliseconds
        Random random = new Random();
        final int id = random.nextInt(); //Used as id to confirm only one handler is running at a time
        h.postDelayed(new Runnable() {
            public void run() {
                Log.d(LOGTAG, id + " Handler runs");
                sendBroadcast(stopwatch.getStringElapsedTime());
                h.postDelayed(this, delay);
            }
        }, delay);
    }

    private void stopHandler() {
        if (h != null) {
            h.removeCallbacksAndMessages(null);
            Log.d(LOGTAG, "Handler destroyed");
        }
    }

    public void restartTimer() {
        stopwatch.run();
        updateTimer();
    }

    public void pauseTimer() {
        //Need to make this cleaner in the stopwatch code
        stopwatch.run();
        updateTimer();
    }

    public void resetTimer() {
        stopwatch.reset();
        stopHandler();
        updateTimer();
    }

    public void updateTimer() {
        sendBroadcast(stopwatch.getStringElapsedTime());
    }
}
