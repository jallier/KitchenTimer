package com.jallier.kitchentimer;

import android.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getFragmentManager().findFragmentById(android.R.id.content) == null) {
            Fragment mainFragment = MainFragment.newInstance("Test");

            getFragmentManager().beginTransaction().add(android.R.id.content, mainFragment).commit();
        }
    }
}
