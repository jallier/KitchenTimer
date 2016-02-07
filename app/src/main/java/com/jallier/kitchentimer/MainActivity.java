package com.jallier.kitchentimer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class MainActivity extends AppCompatActivity {
    private final String LOGTAG = getClass().getSimpleName();
    private final String INTENT_FILTER_TIMERS = "com.jallier.kitchentimer" + ".timers";
    private final String INTENT_EXTRA_TIMER0 = "com.jallier.kitchentimer" + ".timer0";
    private final String INTENT_EXTRA_TIMER1 = "com.jallier.kitchentimer" + ".timer1";
    private final String INTENT_EXTRA_TIMER2 = "com.jallier.kitchentimer" + ".timer2";
    private final String INTENT_EXTRA_TIMER3 = "com.jallier.kitchentimer" + ".timer3";
    private final int NOTIFICATION_ID = 548236;

    private TextView timer0;
    private TextView timer1;
    private TextView timer2;
    private TextView timer3;
    private MainFragment mainFragment;
    private svTimerService myService;
    private BroadcastReceiver timerReceiver;
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
        if (isFinishing()) {
            stopService(serviceIntent);
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        timer0 = (TextView) findViewById(R.id.svTimer0);
        timer1 = (TextView) findViewById(R.id.svTimer1);
        timer2 = (TextView) findViewById(R.id.svTimer2);
        timer3 = (TextView) findViewById(R.id.svTimer3);

        EventBus.getDefault().register(this);

        bindService(serviceIntent, myConnection, Context.BIND_AUTO_CREATE);
        Log.d(LOGTAG, "Service bound");
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
//        unregisterReceiver(timerReceiver);
        unbindService(myConnection);
        Log.d(LOGTAG, "Service unbound");
        super.onStop();
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
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    public void svTimerStateChanged(View view) {
        int viewID = view.getId();
        myService.startTimer(viewID);
    }

    public void resetSvTimer(View view) {
        //This is a messy way to do this, but can't think of a better way rn
        switch (view.getId()) {
            case R.id.svBtnReset0:
                myService.resetTimer(0);
                break;
            case R.id.svBtnReset1:
                myService.resetTimer(1);
                break;
            case R.id.svBtnReset2:
                myService.resetTimer(2);
                break;
            case R.id.svBtnReset3:
                myService.resetTimer(3);
                break;
        }
    }

    @Subscribe
    public void onTimerTickEvent(TimerTickEvent event) {
        Log.d(LOGTAG, "Timer Tick event received");
        timer0.setText(event.getState(INTENT_EXTRA_TIMER0));
        timer1.setText(event.getState(INTENT_EXTRA_TIMER1));
        timer2.setText(event.getState(INTENT_EXTRA_TIMER2));
        timer3.setText(event.getState(INTENT_EXTRA_TIMER3));
    }
}
