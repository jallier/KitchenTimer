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
    private final String INTENT_FILTER_TIMERS = "com.jallier.kitchentimer" + "timers";
    private final String INTENT_EXTRA_TIMER1 = "com.jallier.kitchentimer" + "timer1";
    private final String INTENT_EXTRA_TIMER2 = "com.jallier.kitchentimer" + "timer2";
    private final String INTENT_EXTRA_TIMER3 = "com.jallier.kitchentimer" + "timer3";
    private final String INTENT_EXTRA_TIMER4 = "com.jallier.kitchentimer" + "timer4";

    private TextView timer;
    private MainFragment mainFragment;
    private svTimerService myService;
    private BroadcastReceiver timerReciever;
    private Intent serviceIntent;
    private boolean isBound;

    private ServiceConnection myConnection = new ServiceConnection() { //Create the binder for the service
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

        serviceIntent = new Intent(this, svTimerService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        //myService.stateChanged();
        //stopService(serviceIntent);
        if (isFinishing()) {
            stopService(serviceIntent);
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        timer = (TextView) findViewById(R.id.svTimer);
        //TODO set the intent string values as constants
        timerReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(LOGTAG, "Broadcast recived - textview updated");
                timer.setText(intent.getStringExtra(INTENT_EXTRA_TIMER1));
            }
        };
        IntentFilter intentFilter = new IntentFilter(INTENT_FILTER_TIMERS);
        registerReceiver(timerReciever, intentFilter);

        bindService(serviceIntent, myConnection, Context.BIND_AUTO_CREATE);
        Log.d(LOGTAG, "Service bound");
    }

    @Override
    protected void onStop() {
        unregisterReceiver(timerReciever);

        unbindService(myConnection);
        Log.d(LOGTAG, "Service unbound");
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //myService.stateChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
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

    public void resetSvTimer(View view) {
        myService.resetTimer();
    }
}
