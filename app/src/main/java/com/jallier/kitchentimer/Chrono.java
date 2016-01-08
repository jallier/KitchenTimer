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

    private ChronoState state = ChronoState.STOPPED;
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

    private long setBaseTime(long timeWhenPaused) {
        return SystemClock.elapsedRealtime() + timeWhenPaused;
    }

    /**
     * If the Chrono is stopped; start it. If the Chrono is started; pause it. If the Chrono is started; pause it.
     */
    public void run() {
        if (state == ChronoState.STOPPED) { //Start the timer from stopped state
            super.setBase(SystemClock.elapsedRealtime());
            super.start();
            state = ChronoState.STARTED;
            Log.d(getClass().getSimpleName(), "Timer started");
        } else if (state == ChronoState.STARTED) { //Pause the timer from started state
            timeWhenPaused = super.getBase() - SystemClock.elapsedRealtime();
            super.stop();
            state = ChronoState.PAUSED;
            Log.d(getClass().getSimpleName(), "Timer paused");
        } else if (state == ChronoState.PAUSED) { //Resume the timer from paused state
            super.setBase(setBaseTime(timeWhenPaused));
            super.start();
            state = ChronoState.STARTED;
            Log.d(getClass().getSimpleName(), "Timer started");
        }
    }

    /**
     * Reset the time of the Chrono to 0.
     */
    public void reset() {
        super.stop();
        super.setBase(SystemClock.elapsedRealtime());
        state = ChronoState.STOPPED;
        Log.d(getClass().getSimpleName(), "Timer reset");
    }

    /**
     * Return the state of the Chrono
     *
     * @return enum of state
     */
    public ChronoState getState() {
        return state;
    }

    /**
     * Set the state of the chrono. Used when chrono needs to be restored after device rotation. Only 3 values possible:
     * STARTED, STOPPED or PAUSED
     * @param state string value of enum to set state to.
     */
    public void setState(String state) {
        if (state.equals("STARTED")) {
            this.state = ChronoState.STARTED;
        } else if (state.equals("PAUSED")) {
            this.state = ChronoState.PAUSED;
        } else if (state.equals("STOPPED")) {
            this.state = ChronoState.STOPPED;
        } else {
            Log.w(getClass().getSimpleName(), "Incorrect value passed. Possible values are: 'STARTED', 'STOPPED', or 'PAUSED'");
        }
    }

    /**
     * Start the chrono running again after its previous state has been restored. Chrono will begin running if it was previously, otherwise will remain paused.
     */
    public void resume() {
        if (state == ChronoState.STARTED) {
            super.setBase(setBaseTime(timeWhenPaused));
            super.start();
        } else if (state == ChronoState.PAUSED) {
            super.setBase(setBaseTime(timeWhenPaused));
        }
    }

    /**
     * Return the value of the time when the chrono was paused, in milliseconds
     * @return Time when chrono was paused
     */
    public long getTimeWhenPaused() {
        if (state == ChronoState.STARTED) {
            return super.getBase() - SystemClock.elapsedRealtime();
        } else if (state == ChronoState.PAUSED) {
            return timeWhenPaused;
        }
        return 0;
    }

    /**
     * Used to set the base time of the chrono when restoring state after a saved instance
     * @param time Time when chrono was paused
     */
    public void setTimeWhenPaused(long time) {
        timeWhenPaused = time;
    }
}
