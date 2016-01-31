package com.jallier.kitchentimer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private final String LOGTAG = getClass().getSimpleName();
    private TextView timer;

    private MainFragment mainFragment;
    private svTimerService myService;
    private boolean isBound;

    private ServiceConnection myConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            svTimerService.MyBinder binder = (svTimerService.MyBinder) service;
            myService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

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

        Intent intent = new Intent(this, svTimerService.class);
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
        Log.d(LOGTAG, "Service bound");
    }

    @Override
    protected void onDestroy() {
        myService.unbindService(myConnection);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        timer = (TextView) findViewById(R.id.svTimer);
        //TODO Unregister this in onPause()
        BroadcastReceiver timerReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(LOGTAG, "Broadcast recived");
                timer.setText(intent.getStringExtra("value"));
            }
        };
        IntentFilter intentFilter = new IntentFilter("timerIntent");
        registerReceiver(timerReciever, intentFilter);
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

    public void startSVTimer(View view) {
        Stopwatch.TimerState state = myService.getTimerState();
        if (state == Stopwatch.TimerState.STOPPED) {
            myService.startTimer();
        } else if (state == Stopwatch.TimerState.PAUSED) {
            myService.restartTimer();
        } else if (state == Stopwatch.TimerState.STARTED) {
            myService.pauseTimer();
        }
    }
}
