package com.jallier.kitchentimer;

import android.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    private MainFragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getFragmentManager().findFragmentById(android.R.id.content) == null) {
            mainFragment = MainFragment.newInstance("Test");

            getFragmentManager().beginTransaction().add(android.R.id.content, mainFragment).commit();
        }
    }
    public void startTimer(View view) {
        Log.d(getClass().getSimpleName(), "Timer started");
        mainFragment.buttonPressed();
    }
}
