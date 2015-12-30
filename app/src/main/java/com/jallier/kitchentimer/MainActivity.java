package com.jallier.kitchentimer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    private MainFragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getFragmentManager().findFragmentById(android.R.id.content) == null) { //If fragment does not exist, create it.
            mainFragment = MainFragment.newInstance("Test");
            getFragmentManager().beginTransaction().add(android.R.id.content, mainFragment).commit();
        }
    }

    public void startTimer(View view) { //Triggered from tapping on time.
        mainFragment.startOrPauseTimer();
    }

    public void resetTimer(View view) { //Triggered from reset Button
        mainFragment.resetTimer();
    }
}
