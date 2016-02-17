package com.jallier.kitchentimer;

import android.graphics.Color;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

class Stopwatch {
    public Stopwatch() {
        elapsedTime = 0;
        colorCounter = true; //Used to blink the timer when paused
    }

    private final String LOGTAG = getClass().getSimpleName();

    private long startTime;
    private long elapsedTime;
    private TimerState state = TimerState.STOPPED;
    private boolean colorCounter;

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
        colorCounter = true;
    }

    public void reset() {
        elapsedTime = 0;
        colorCounter = true;
        state = TimerState.STOPPED;
        Log.d(LOGTAG, "Timer reset");
    }

    public long getElapsedTime() {
        if (state == TimerState.STOPPED || state == TimerState.PAUSED) {
            if (state == TimerState.PAUSED) {
                colorCounter = !colorCounter;
            }
            return elapsedTime;
        } else { //calculate elapsed time, but do not set the variable
            return elapsedTime + (SystemClock.elapsedRealtime() - startTime);
        }
    }

    public String getStringElapsedTime() {
        long elapsedTime = getElapsedTime();
        int hour, minute, second;

        hour = (int) (elapsedTime / 1000) / 3600;
        minute = (int) ((elapsedTime / 1000) / 60) - (hour * 60);
        second = (int) (elapsedTime / 1000) - (hour * 3600) - (minute * 60);

        //return String.format((hour + ":" + minute + ":" + second), );
        return String.format("%2d:%02d:%02d", hour, minute, second);
    }

    public TimerState getState() {
        return state;
    }

    public int isTimerVisible() {
        if (colorCounter) {
            return 1; //Timer is visible - opacity set to 1
        } else {
            return 0; //Timer invisible - opacity set to 0
        }
    }

    public long getStartTime() {
        return startTime;
    }
}
