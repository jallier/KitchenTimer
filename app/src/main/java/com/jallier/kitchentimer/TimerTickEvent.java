package com.jallier.kitchentimer;

import android.support.v4.util.SimpleArrayMap;

public class TimerTickEvent {
    private SimpleArrayMap<String, String> states;

    public TimerTickEvent() {
        states = new SimpleArrayMap<>();
    }

    public void addState(String key, String value) {
        states.put(key, value);
    }

    public String getState(String key) {
        return states.get(key);
    }
}
