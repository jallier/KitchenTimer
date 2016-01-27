package com.jallier.kitchentimer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
        } else {
            mainFragment = (MainFragment)getFragmentManager().findFragmentById(android.R.id.content);
        }
        //Log.d(getClass().getSimpleName(), "Activity created");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, Preferences.class));
        }
        return super.onOptionsItemSelected(item);
    }

    public void startTimer(View view) { //Triggered from tapping on timer.
        mainFragment.startOrPauseTimer(view);
    }

    public void resetTimer(View view) { //Triggered from reset Button
        mainFragment.resetTimer(view);
    }

    public void resetAll(View view) { //Triggered from reset all button
        mainFragment.resetAll(view);
    }
}
