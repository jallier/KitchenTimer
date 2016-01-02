package com.jallier.kitchentimer;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Chronometer;

/**
 * Extends the Chronometer class to provide basic starting, pausing and resetting logic.
 */
public class Chrono extends Chronometer {
    /**
     * States that the Chrono can exist in. Default is Stopped
     */
    private enum chronoState {
        STARTED,STOPPED, PAUSED
    }

    private chronoState state = chronoState.STOPPED;
    private long timeWhenPaused = 0;


    public Chrono(Context context) {
        super(context);
    }

    public Chrono(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Chrono(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public Chrono(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * If the Chrono is stopped; start it. If the Chrono is started; pause it. If the Chrono is started; pause it.
     */
    public void run() {
        if (state == chronoState.STOPPED) { //Start the timer from stopped state
            super.setBase(SystemClock.elapsedRealtime());
            super.start();
            state = chronoState.STARTED;
            Log.d(getClass().getSimpleName(), "Timer started");
        } else if (state == chronoState.STARTED) { //Pause the timer from started state
            timeWhenPaused = super.getBase() - SystemClock.elapsedRealtime();
            super.stop();
            state = chronoState.PAUSED;
            Log.d(getClass().getSimpleName(), "Timer paused");
        } else if (state == chronoState.PAUSED) { //Resume the timer from paused state
            super.setBase(SystemClock.elapsedRealtime() + timeWhenPaused);
            super.start();
            state = chronoState.STARTED;
            Log.d(getClass().getSimpleName(), "Timer started");
        }
    }

    /**
     * Reset the time of the Chrono to 0.
     */
    public void reset() {
        super.stop();
        super.setBase(SystemClock.elapsedRealtime());
        state = chronoState.STOPPED;
        Log.d(getClass().getSimpleName(), "Timer reset");
    }
}
