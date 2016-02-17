package com.jallier.kitchentimer;

import android.support.v4.util.SimpleArrayMap;

class TimerTickEvent {
    private SimpleArrayMap<String, String> elapsedTime;
    private SimpleArrayMap<String, Integer> timerVisible;

    public TimerTickEvent() {
        elapsedTime = new SimpleArrayMap<>();
        timerVisible = new SimpleArrayMap<>();
    }

    public void setElapsed(String key, String value) {
        elapsedTime.put(key, value);
    }

    public String getElapsed(String key) {
        return elapsedTime.get(key);
    }

    public void setVisibility(String key, int state) {
        timerVisible.put(key, state);
    }

    public int getVisibility(String key) {
        return timerVisible.get(key);
    }
}
