package com.jallier.kitchentimer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;


public class Preferences extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new Display())
                .commit();
    }

    public static class Display extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        String defaultSummary;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            Preference interval = findPreference(MainActivity.PREF_SPEAK_INTERVAL);
            defaultSummary = interval.getSummary().toString();
            interval.setSummary(""
                    + prefs.getString(MainActivity.PREF_SPEAK_INTERVAL, "1")
                    + " minutes - "
                    + defaultSummary);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(MainActivity.PREF_SPEAK_INTERVAL)) {
                Preference interval = findPreference(key);
                interval.setSummary(""
                        + sharedPreferences.getString(MainActivity.PREF_SPEAK_INTERVAL, "1")
                        + " minutes - "
                        + defaultSummary);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }
}
