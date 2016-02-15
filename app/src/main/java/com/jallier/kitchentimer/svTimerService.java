package com.jallier.kitchentimer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class svTimerService extends Service {
    private final String LOGTAG = getClass().getSimpleName();
    private final IBinder myBinder = new MyBinder();

    private Stopwatch[] stopwatches;
    private int[] stopwatchTTSTimeCounter;
    private TTSTimerTask[] ttsTimerTask;
    private Timer ttsTimer;
    private ScheduledThreadPoolExecutor executor;
    private NotificationCompat.Builder notifBuilder;
    private NotificationCompat.BigTextStyle big;
    private boolean executorRunning = false;
    private boolean serviceBound = true;
    private TTSHelper textToSpeechHelper;
    private SharedPreferences sharedPrefs;
    private long ttsInterval;
    private int ttsIntervalMinutes;

    public svTimerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOGTAG, "service binds");
        serviceBound = true;
        //Update sharedprefs and assign interval value
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        ttsIntervalMinutes = Integer.parseInt(sharedPrefs.getString(MainActivity.PREF_SPEAK_INTERVAL, "5"));
        ttsInterval = ttsIntervalMinutes * 60 * 1000; //Convert to ms
        return myBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        serviceBound = true;
        //Update sharedprefs and assign interval value
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        ttsIntervalMinutes = Integer.parseInt(sharedPrefs.getString(MainActivity.PREF_SPEAK_INTERVAL, "5"));
        ttsInterval = ttsIntervalMinutes * 60 * 1000; //Convert to ms
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
        ttsTimerTask = new TTSTimerTask[5];
        ttsTimer = new Timer();

        buildNotification();
        textToSpeechHelper = new TTSHelper(getApplicationContext());
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

    private class TTSTimerTask extends TimerTask {
        TTSHelper tts;
        int timerID;

        public TTSTimerTask(TTSHelper tts, int timerID) {
            this.tts = tts;
            this.timerID = timerID;
        }

        private String convertMinutesToHoursString(int minutes) {
            String outputString = "";
            int hours, minutesRemaining;
            hours = (int) Math.floor(minutes / 60.0);
            minutesRemaining = minutes % 60;
            //Hours formatting
            if (hours < 1) {
                if (minutesRemaining == 1) {
                    outputString += (minutesRemaining + " minute elapsed");
                } else {
                    outputString += (minutesRemaining + " minutes elapsed");
                }
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
        public void run() {
            Log.d(LOGTAG, "TimerTask runs - ID " + timerID);
            //Only announce the time if the sharedpref is set to true
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); //Update sharedPrefs
            if (sharedPrefs.getBoolean(MainActivity.PREF_SPEAK_ELAPSED, false)) {
                String output = "Timer " + (timerID + 1) + ". " + convertMinutesToHoursString(stopwatchTTSTimeCounter[timerID]);
                stopwatchTTSTimeCounter[timerID] += ttsIntervalMinutes;
                tts.speak(output);
            }
        }
    }

    public void startTimer(int viewID) {
        stopwatchTTSTimeCounter = new int[]{ttsIntervalMinutes, ttsIntervalMinutes, ttsIntervalMinutes, ttsIntervalMinutes, ttsIntervalMinutes};
        int timerID;
        switch (viewID) {
            case R.id.svTimer0:
                timerID = 0;
                scheduleTimerTask(timerID);
                stopwatches[0].run();
                break;
            case R.id.svTimer1:
                timerID = 1;
                scheduleTimerTask(timerID);
                stopwatches[1].run();
                break;
            case R.id.svTimer2:
                timerID = 2;
                scheduleTimerTask(timerID);
                stopwatches[2].run();
                break;
            case R.id.svTimer3:
                timerID = 3;
                scheduleTimerTask(timerID);
                stopwatches[3].run();
                break;
            case R.id.svTimer4:
                timerID = 4;
                scheduleTimerTask(timerID);
                stopwatches[4].run();
                break;
        }
        if (!executorRunning) {
            startExecutor();
        }
    }

    private void scheduleTimerTask(int timerID) {
        long interval = ttsInterval;
        long scheduleAt;

        TimerState state = stopwatches[timerID].getState();
        if (state == TimerState.STOPPED) {
            //Start a tts task
            ttsTimerTask[timerID] = new TTSTimerTask(textToSpeechHelper, timerID);
            ttsTimer.schedule(ttsTimerTask[timerID], interval, interval);
        } else if (state == TimerState.STARTED) {
            //cancel current task
            ttsTimerTask[timerID].cancel();
        } else {
            //Work out the time until the next timer should be scheduled, then schedule it
            scheduleAt = interval - (stopwatches[timerID].getElapsedTime() % interval);
            ttsTimerTask[timerID] = new TTSTimerTask(textToSpeechHelper, timerID);
            ttsTimer.schedule(ttsTimerTask[timerID], scheduleAt, interval);
        }
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
            //TODO: This can probably be condensed
            case 0:
                stopwatches[0].reset();
                if (ttsTimerTask[0] != null) {
                    ttsTimerTask[0].cancel();
                    stopwatchTTSTimeCounter[0] = ttsIntervalMinutes;
                }
                break;
            case 1:
                stopwatches[1].reset();
                if (ttsTimerTask[1] != null) {
                    ttsTimerTask[1].cancel();
                    stopwatchTTSTimeCounter[1] = ttsIntervalMinutes;
                }
                break;
            case 2:
                stopwatches[2].reset();
                if (ttsTimerTask[2] != null) {
                    ttsTimerTask[2].cancel();
                    stopwatchTTSTimeCounter[2] = ttsIntervalMinutes;
                }
                break;
            case 3:
                stopwatches[3].reset();
                if (ttsTimerTask[3] != null) {
                    ttsTimerTask[3].cancel();
                    stopwatchTTSTimeCounter[3] = ttsIntervalMinutes;
                }
                break;
            case 4:
                stopwatches[4].reset();
                if (ttsTimerTask[4] != null) {
                    ttsTimerTask[4].cancel();
                    stopwatchTTSTimeCounter[4] = ttsIntervalMinutes;
                }
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
