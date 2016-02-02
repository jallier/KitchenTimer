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
    private final String INTENT_EXTRA_TIMER0 = "com.jallier.kitchentimer" + ".timer0";
    private final String INTENT_EXTRA_TIMER1 = "com.jallier.kitchentimer" + ".timer1";
    private final String INTENT_EXTRA_TIMER2 = "com.jallier.kitchentimer" + ".timer2";
    private final String INTENT_EXTRA_TIMER3 = "com.jallier.kitchentimer" + ".timer3";
    private final IBinder myBinder = new MyBinder();

    private Stopwatch[] stopwatches;
    private Handler h;
    private boolean handlerRunning = false;

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
        stopwatches = new Stopwatch[]{
                new Stopwatch(),
                new Stopwatch(),
                new Stopwatch(),
                new Stopwatch()
        };
    }

    @Override
    public void onDestroy() {
        stopHandler();
        Log.d(getClass().getSimpleName(), "Service stopped");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateTimers();
        return super.onStartCommand(intent, flags, startId);
    }

    private void sendBroadcast(String[] timerValues) {
        Intent intent = new Intent(INTENT_FILTER_TIMERS);
        intent.putExtra(INTENT_EXTRA_TIMER0, timerValues[0]);
        intent.putExtra(INTENT_EXTRA_TIMER1, timerValues[1]);
        intent.putExtra(INTENT_EXTRA_TIMER2, timerValues[2]);
        intent.putExtra(INTENT_EXTRA_TIMER3, timerValues[3]);
        sendBroadcast(intent);
    }

    public TimerState[] getTimerStates() {
        TimerState[] states = new TimerState[4];
        int i = 0;
        for (Stopwatch stopwatch : stopwatches) {
            states[i] = stopwatch.getState();
            i++;
        }
        return states;
    }

    public void startTimer(int viewID) {
        switch (viewID) {
            case R.id.svTimer0:
                stopwatches[0].run();
                break;
            case R.id.svTimer1:
                stopwatches[1].run();
                break;
            case R.id.svTimer2:
                stopwatches[2].run();
                break;
            case R.id.svTimer3:
                stopwatches[3].run();
                break;
        }
        if (!handlerRunning) {
            startHandler();
        }
    }

    private void startHandler() {
        //Handler fires off a broadcast intent every 1000ms, which is received in the activity and updates the textview
        handlerRunning = true;
        Log.d(LOGTAG, "Service started");
        h = new Handler();
        final int delay = 500; //milliseconds
        Random random = new Random();
        final int id = random.nextInt(); //Used as id to confirm only one handler is running at a time
        h.postDelayed(new Runnable() {
            public void run() {
                Log.d(LOGTAG, id + " Handler runs");
                //sendBroadcast(stopwatches.getStringElapsedTime());
                updateTimers();
                h.postDelayed(this, delay);
            }
        }, delay);
    }

    private void stopHandler() {
        if (h != null) {
            h.removeCallbacksAndMessages(null);
            handlerRunning = false;
            Log.d(LOGTAG, "Handler destroyed");
        }
    }

    public void restartTimer(int viewID) {
        //TODO: Restructure timer code to use different methods for start/pause/reset
        switch (viewID) {
            case R.id.svTimer0:
                stopwatches[0].run();
                break;
            case R.id.svTimer1:
                stopwatches[1].run();
                break;
            case R.id.svTimer2:
                stopwatches[2].run();
                break;
            case R.id.svTimer3:
                stopwatches[3].run();
                break;
        }
        updateTimers();
    }

    public void pauseTimer(int viewID) {
        //Need to make this cleaner in the stopwatches code
        switch (viewID) {
            case R.id.svTimer0:
                stopwatches[0].run();
                break;
            case R.id.svTimer1:
                stopwatches[1].run();
                break;
            case R.id.svTimer2:
                stopwatches[2].run();
                break;
            case R.id.svTimer3:
                stopwatches[3].run();
                break;
        }
        updateTimers();
    }

    public void resetTimer(int buttonID) {
        //TODO: FIX THIS
        switch (buttonID) {
            case 0:
                stopwatches[0].reset();
                break;
            case 1:
                stopwatches[1].reset();
                break;
            case 2:
                stopwatches[2].reset();
                break;
            case 3:
                stopwatches[3].reset();
                break;
        }
        //Check if any timers are running before stopping the handler
        int numberOfTimersRunning = 0;
        TimerState[] states = getTimerStates();
        for (TimerState state : states) {
            if (state == TimerState.STARTED || state == TimerState.PAUSED) {
                numberOfTimersRunning++;
            }
        }
        if (numberOfTimersRunning < 1) {
            stopHandler();
        }
        updateTimers();
    }

    public void updateTimers() {
        String[] elapsedTimeValues = new String[4];
        int i = 0;
        for (Stopwatch stopwatch : stopwatches) {
            elapsedTimeValues[i] = stopwatch.getStringElapsedTime();
            i++;
        }
        sendBroadcast(elapsedTimeValues);
    }
}
