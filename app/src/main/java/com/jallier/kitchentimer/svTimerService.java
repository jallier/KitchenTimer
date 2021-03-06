package com.jallier.kitchentimer;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class svTimerService extends Service {
    private final String LOGTAG = getClass().getSimpleName();
    private final IBinder myBinder = new MyBinder();
    private final AlarmReceiver alarmReceiver = new AlarmReceiver();
    private final String INTENT_TIMER_BASE = "TIMER";
    private final String INTENT_TIMER0 = "TIMER0";
    private final String INTENT_TIMER1 = "TIMER1";
    private final String INTENT_TIMER2 = "TIMER2";
    private final String INTENT_TIMER3 = "TIMER3";
    private final String INTENT_TIMER4 = "TIMER4";

    private Stopwatch[] stopwatches;
    private int[] stopwatchTTSTimeCounter;
    private ScheduledThreadPoolExecutor executor;
    private NotificationCompat.Builder notifBuilder;
    private NotificationCompat.BigTextStyle big;
    private boolean executorRunning = false;
    private boolean serviceBound = true;
    private TTSHelper textToSpeechHelper;
    private SharedPreferences sharedPrefs;
    private long ttsInterval;
    private int ttsIntervalMinutes;
    private PendingIntent[] pendingIntents;
    private AlarmManager alarmManager;

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
        stopwatchTTSTimeCounter = new int[]{ttsIntervalMinutes, ttsIntervalMinutes, ttsIntervalMinutes, ttsIntervalMinutes, ttsIntervalMinutes};
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
        pendingIntents = new PendingIntent[stopwatches.length];

        buildNotification();
        textToSpeechHelper = new TTSHelper(getApplicationContext());

        //Add intent filters for tts notifications
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(INTENT_TIMER0);
        intentFilter.addAction(INTENT_TIMER1);
        intentFilter.addAction(INTENT_TIMER2);
        intentFilter.addAction(INTENT_TIMER3);
        intentFilter.addAction(INTENT_TIMER4);
        registerReceiver(alarmReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        stopExecutor();
        stopForeground(true);
        textToSpeechHelper.shutdown();
        unregisterReceiver(alarmReceiver);
        Log.d(LOGTAG, "Service destroyed");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceBound = true;
        updateTimers();
        return super.onStartCommand(intent, flags, startId);
    }

    public void startTimer(int timerID) {
        Intent intent = new Intent(INTENT_TIMER_BASE + timerID);
        scheduleTimerTask(timerID, intent);
        stopwatches[timerID].run();

        if (!executorRunning) {
            startExecutor();
        }
    }

    private void scheduleTimerTask(int timerID, Intent intent) {
        long interval = ttsInterval;
        long scheduleAt;

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        pendingIntents[timerID] = PendingIntent.getBroadcast(getApplicationContext(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        TimerState state = stopwatches[timerID].getState();
        if (state == TimerState.STOPPED) {
            //Start a tts task
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + interval, pendingIntents[timerID]);
        } else if (state == TimerState.STARTED) {
            //cancel current task
            alarmManager.cancel(pendingIntents[timerID]);
        } else {
            //Work out the time until the next timer should be scheduled, then schedule it
            scheduleAt = interval - (stopwatches[timerID].getElapsedTime() % interval);
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + scheduleAt, pendingIntents[timerID]);
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

    public void resetTimer(int buttonID) {
        stopwatches[buttonID].reset();
        if (pendingIntents[buttonID] != null) {
            stopwatchTTSTimeCounter[buttonID] = ttsIntervalMinutes;
            alarmManager.cancel(pendingIntents[buttonID]);
        }

        //Check if any timers are running before stopping the handler
        if (numberOfTimersRunning() < 1) {
            stopExecutor();
        }
        updateTimers();
        //textToSpeechHelper.speak("Timer reset");
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

    public TimerState[] getTimerStates() {
        TimerState[] states = new TimerState[stopwatches.length];
        int i = 0;
        for (Stopwatch stopwatch : stopwatches) {
            states[i] = stopwatch.getState();
            i++;
        }
        return states;
    }

    private void postEventToActivity(String[] timerValues, int[] visibilityValues) {
        TimerTickEvent event = new TimerTickEvent();
        event.setElapsed(MainActivity.INTENT_EXTRA_TIMER0, timerValues[0]);
        event.setElapsed(MainActivity.INTENT_EXTRA_TIMER1, timerValues[1]);
        event.setElapsed(MainActivity.INTENT_EXTRA_TIMER2, timerValues[2]);
        event.setElapsed(MainActivity.INTENT_EXTRA_TIMER3, timerValues[3]);
        event.setElapsed(MainActivity.INTENT_EXTRA_TIMER4, timerValues[4]);

        event.setVisibility(MainActivity.INTENT_EXTRA_TIMER0, visibilityValues[0]);
        event.setVisibility(MainActivity.INTENT_EXTRA_TIMER1, visibilityValues[1]);
        event.setVisibility(MainActivity.INTENT_EXTRA_TIMER2, visibilityValues[2]);
        event.setVisibility(MainActivity.INTENT_EXTRA_TIMER3, visibilityValues[3]);
        event.setVisibility(MainActivity.INTENT_EXTRA_TIMER4, visibilityValues[4]);
        EventBus.getDefault().post(event);
    }

    private void updateTimers() {
        String[] elapsedTimeValues = new String[stopwatches.length];
        int[] visibilityValues = new int[stopwatches.length];
        int i = 0;
        for (Stopwatch stopwatch : stopwatches) {
            elapsedTimeValues[i] = stopwatch.getStringElapsedTime();
            visibilityValues[i] = stopwatch.isTimerVisible();
            i++;
        }
        if (serviceBound) {
            postEventToActivity(elapsedTimeValues, visibilityValues);
        } else {
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
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    private class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOGTAG, "Alarm received");
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            final PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Alarm");
            wakeLock.acquire();

            String action = intent.getAction();
            int timerID = Integer.parseInt(action.substring(action.length() - 1));
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()); //Update sharedPrefs
            if (sharedPrefs.getBoolean(MainActivity.PREF_SPEAK_ELAPSED, false)) {
                String output = "Timer " + (timerID + 1) + ". " + convertMinutesToHoursString(stopwatchTTSTimeCounter[timerID]);
                stopwatchTTSTimeCounter[timerID] += ttsIntervalMinutes;
                textToSpeechHelper.speak(output);

                //Might be best to have this fire every time regardless, so if tts is enabled half way through, it will work.
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ttsInterval, pendingIntents[timerID]);
                wakeLock.release();
            }
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
    }
}
