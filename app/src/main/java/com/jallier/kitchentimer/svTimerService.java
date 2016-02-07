package com.jallier.kitchentimer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.Random;

public class svTimerService extends Service {
    private final String LOGTAG = getClass().getSimpleName();
    private final String INTENT_FILTER_TIMERS = "com.jallier.kitchentimer" + ".timers";
    private final String INTENT_EXTRA_TIMER0 = "com.jallier.kitchentimer" + ".timer0";
    private final String INTENT_EXTRA_TIMER1 = "com.jallier.kitchentimer" + ".timer1";
    private final String INTENT_EXTRA_TIMER2 = "com.jallier.kitchentimer" + ".timer2";
    private final String INTENT_EXTRA_TIMER3 = "com.jallier.kitchentimer" + ".timer3";
    private final int NOTIFICATION_ID = 548236;
    private final IBinder myBinder = new MyBinder();

    private Stopwatch[] stopwatches;
    private Handler h;
    private NotificationCompat.Builder notifBuilder;
    private NotificationCompat.BigTextStyle big;
    private boolean handlerRunning = false;
    private boolean serviceBound = true;

    public svTimerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOGTAG, "service binds");
        return myBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        serviceBound = true;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (numberOfTimersRunning() != 0) {
            startForeground(NOTIFICATION_ID, raiseNotif(true));
        }
        serviceBound = false;

        //Return true so that onRebind is called
        super.onUnbind(intent);
        return true;
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
        buildNotification();
    }

    @Override
    public void onDestroy() {
        stopHandler();
        stopForeground(true);
        Log.d(getClass().getSimpleName(), "Service stopped");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateTimers();
        return super.onStartCommand(intent, flags, startId);
    }

    private void sendBroadcast(String[] timerValues) {
        //TODO: Check if broadcast intents are the best/most efficient way to do this vs event bus. I think the broadcast reciever is causing lag

        TimerTickEvent event = new TimerTickEvent();
        event.addState(INTENT_EXTRA_TIMER0, timerValues[0]);
        event.addState(INTENT_EXTRA_TIMER1, timerValues[1]);
        event.addState(INTENT_EXTRA_TIMER2, timerValues[2]);
        event.addState(INTENT_EXTRA_TIMER3, timerValues[3]);
        EventBus.getDefault().post(event);
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
        //TODO: make this cleaner in the stopwatches code
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
        if (numberOfTimersRunning() < 1) {
            stopHandler();
        }
        updateTimers();
    }

    private int numberOfTimersRunning() {
        int i = 0;
        TimerState[] states = getTimerStates();
        for (TimerState state : states) {
            if (state == TimerState.STARTED || state == TimerState.PAUSED) {
                i++;
            }
        }
        return i;
    }

    public void updateTimers() {
        String[] elapsedTimeValues = new String[4];
        int i = 0;
        for (Stopwatch stopwatch : stopwatches) {
            elapsedTimeValues[i] = stopwatch.getStringElapsedTime();
            i++;
        }
        sendBroadcast(elapsedTimeValues);
        if (!serviceBound) {
            raiseNotif(false);
        }
    }

    private void buildNotification() {
        notifBuilder = new NotificationCompat.Builder(this);

        //Regular small style notification
        notifBuilder.setAutoCancel(true)
                .setContentTitle(getString(R.string.notifTimersRunning))
                .setContentIntent(buildPendingIntent())
                .addAction(0, getString(R.string.notifAction), buildPendingIntent()) //Set icon to 0 to remove it.
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true);

        big = new NotificationCompat.BigTextStyle();
    }

    @Nullable
    private Notification raiseNotif(boolean startInForeground) {
        NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //Regular small style notification
        notifBuilder.setContentText(numberOfTimersRunning() + " " + getString(R.string.notifXTimersRunning));

        //Expanded style notification
        int i = 1;
        String notifContent = "";
        for (Stopwatch stopwatch : stopwatches) { //Add timers to notification
            notifContent += getString(R.string.notifTimersNumber) + " " + i + " - " + stopwatch.getStringElapsedTime();
            notifContent += "\n";
            i++;
        }
        big.bigText(notifContent);
        notifBuilder.setStyle(big);
        mgr.notify(NOTIFICATION_ID, notifBuilder.build());

        if (startInForeground) {
            return notifBuilder.build();
        } else {
            return null;
        }
    }

    private PendingIntent buildPendingIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        //Not sure if these two setters are needed.
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }
}
