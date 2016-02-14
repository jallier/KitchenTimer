package com.jallier.kitchentimer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class svTimerService extends Service {
    private final String LOGTAG = getClass().getSimpleName();
    private final String INTENT_FILTER_ALARM = "com.jallier.kitchentimer" + ".alarmFilter";
    private final String INTENT_EXTRA_TTS_TIMER_ID = "com.jallier.kitchentimer" + ".tts";
    private final IBinder myBinder = new MyBinder();

    private Stopwatch[] stopwatches;
    private int[] stopwatchesTTSCounter; //Count elapsed minutes for TTS announcements
    private ScheduledThreadPoolExecutor executor;
    private NotificationCompat.Builder notifBuilder;
    private NotificationCompat.BigTextStyle big;
    private boolean executorRunning = false;
    private boolean serviceBound = true;
    private TTSHelper textToSpeechHelper;
    private BroadcastReceiver alarmReceiver;

    public svTimerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOGTAG, "service binds");
        serviceBound = true;
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
            startForeground(MainActivity.NOTIFICATION_ID, raiseNotification(true));
            Log.d(LOGTAG, "Service started in foreground");
            serviceBound = false;
        } else { //Destroy the service
            stopSelf();
        }

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
                new Stopwatch(),
                new Stopwatch()
        };
        stopwatchesTTSCounter = new int[]{0, 0, 0, 0, 0};
        buildNotification();
        textToSpeechHelper = new TTSHelper(getApplicationContext());
        alarmReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(LOGTAG, "Alarm broadcast received");
                handleReceivedAlarm(intent);
            }
        };
        IntentFilter intentFilter = new IntentFilter(INTENT_FILTER_ALARM);
        registerReceiver(alarmReceiver, intentFilter);
    }

    //TODO: Move this method somewhere else when done
    private void handleReceivedAlarm(Intent intent) {
        int timerID = intent.getIntExtra(INTENT_EXTRA_TTS_TIMER_ID, -1);
        switch (timerID) { //Build tts string depending on which timer, and how many elapsed minutes
            case -1:
                //Do something here
            case 4:
                textToSpeechHelper.speak("Timer " + (timerID + 1) + ". " + convertMinutesToHoursString(stopwatchesTTSCounter[timerID]));
                stopwatchesTTSCounter[timerID] += 5;
        }
    }

    private String convertMinutesToHoursString(int minutes) {
        String outputString = "";
        int hours, minutesRemaining;
        hours = (int) Math.floor(minutes / 60.0);
        minutesRemaining = minutes % 60;
        //Hours formatting
        if (hours < 1) {
            outputString += (minutesRemaining + " minutes elapsed");
            return outputString;
        } else if (hours == 1) {
            outputString += (hours + " hour ");
        } else {
            outputString += (hours + " hours ");
        }

        //Minutes formatting
        if (minutesRemaining != 0) {
            outputString += (minutesRemaining + " minutes ");
        }

        outputString += "elapsed";

        return outputString;
    }

    @Override
    public void onDestroy() {
        stopExecutor();
        stopForeground(true);
        textToSpeechHelper.shutdown();
        Log.d(getClass().getSimpleName(), "Service stopped");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceBound = true;
        updateTimers();
        return super.onStartCommand(intent, flags, startId);
    }

    private void postEventToActivity(String[] timerValues) {
        TimerTickEvent event = new TimerTickEvent();
        event.addState(MainActivity.INTENT_EXTRA_TIMER0, timerValues[0]);
        event.addState(MainActivity.INTENT_EXTRA_TIMER1, timerValues[1]);
        event.addState(MainActivity.INTENT_EXTRA_TIMER2, timerValues[2]);
        event.addState(MainActivity.INTENT_EXTRA_TIMER3, timerValues[3]);
        event.addState(MainActivity.INTENT_EXTRA_TIMER4, timerValues[4]);
        EventBus.getDefault().post(event);
    }

    public TimerState[] getTimerStates() {
        TimerState[] states = new TimerState[stopwatches.length];
        int i = 0;
        for (Stopwatch stopwatch : stopwatches) {
            states[i] = stopwatch.getState();
            i++;
        }
        return states;
    }

    public void startTimer(int viewID) {
        Intent intent = new Intent(INTENT_FILTER_ALARM);
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
            case R.id.svTimer4:
                stopwatches[4].run();
                intent.putExtra(INTENT_EXTRA_TTS_TIMER_ID, 4);
                break;
        }
        if (!executorRunning) {
            startExecutor();
        }
        
        sendBroadcast(intent);
    }

    private void startExecutor() {
        //Executor fires off a broadcast intent every 1000ms, which is received in the activity and updates the textview
        executorRunning = true;
        Log.d(LOGTAG, "Service started");
        final int delay = 500; //milliseconds
        Random random = new Random();
        final int id = random.nextInt(); //Used as id to confirm only one handler is running at a time

        executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Log.d(LOGTAG, id + " executor runs");
                updateTimers();
            }
        }, 0, delay, TimeUnit.MILLISECONDS);
    }

    private void stopExecutor() {
        if (executor != null) {
            executor.shutdown();
            executorRunning = false;
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
            case 4:
                stopwatches[4].reset();
                break;
        }
        //Check if any timers are running before stopping the handler
        if (numberOfTimersRunning() < 1) {
            stopExecutor();
        }
        updateTimers();
        textToSpeechHelper.speak("Timer reset");
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
        String[] elapsedTimeValues = new String[stopwatches.length];
        int i = 0;
        for (Stopwatch stopwatch : stopwatches) {
            elapsedTimeValues[i] = stopwatch.getStringElapsedTime();
            i++;
        }
        postEventToActivity(elapsedTimeValues);
        if (!serviceBound) {
            raiseNotification(false);
        }
    }

    private void buildNotification() {
        notifBuilder = new NotificationCompat.Builder(this);

        //Regular small style notification
        notifBuilder.setAutoCancel(true)
                .setContentTitle(getString(R.string.notifTimersRunning))
                .setContentIntent(buildPendingIntent())
                .addAction(0, getString(R.string.notifAction), buildPendingIntent()) //Set icon to 0 to remove it.
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true);

        big = new NotificationCompat.BigTextStyle();
    }

    @Nullable
    private Notification raiseNotification(boolean startInForeground) {
        NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //Regular small style notification
        notifBuilder.setContentText(numberOfTimersRunning() + " " + getString(R.string.notifXTimersRunning));

        //Expanded style notification
        int i = 1;
        String notifContent = "";
        for (Stopwatch stopwatch : stopwatches) { //Add timers to notification
            if (stopwatch.getState() != TimerState.STOPPED) {
                notifContent += getString(R.string.notifTimersNumber) + " " + i + " - " + stopwatch.getStringElapsedTime();
                notifContent += "\n";
            }
            i++;
        }
        notifContent = notifContent.trim(); //Remove last newline char
        big.bigText(notifContent);
        notifBuilder.setStyle(big);
        mgr.notify(MainActivity.NOTIFICATION_ID, notifBuilder.build());

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
