package com.jallier.kitchentimer;

import android.os.SystemClock;
import android.util.Log;

public class Timer {
    public enum TimerState {
        STOPPED, STARTED, PAUSED
    }

    public Timer() {
        elapsedTime = 0;
    }

    private final String LOGTAG = getClass().getSimpleName();

    private long startTime;
    private long elapsedTime;
    private TimerState state = TimerState.STOPPED;

    public void run() {
        if (state == TimerState.STOPPED) {
            startTime = SystemClock.elapsedRealtime();
            elapsedTime = 0;
            state = TimerState.STARTED;
            Log.d(LOGTAG, "Timer started");
        } else if (state == TimerState.STARTED) { //Pause timer and set elapsed time
            elapsedTime = elapsedTime + (SystemClock.elapsedRealtime() - startTime);
            state = TimerState.PAUSED;
            Log.d(LOGTAG, "Timer paused");
        } else if (state == TimerState.PAUSED) {
            startTime = SystemClock.elapsedRealtime();
            state = TimerState.STARTED;
            Log.d(LOGTAG, "Timer resumed");
        }
    }

    public void reset() {
        elapsedTime = 0;
        state = TimerState.STOPPED;
        Log.d(LOGTAG, "Timer reset");
    }

    public long getElapsedTime() {
        if (state == TimerState.STOPPED) {
            return elapsedTime;
        } else { //calculate elapsed time, but do not set the variable
            return elapsedTime + (SystemClock.elapsedRealtime() - startTime);
        }
    }
}
