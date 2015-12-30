package com.jallier.kitchentimer;

import android.app.Fragment;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "First timer";

    private Chronometer chrono;
    private TimerState timerState;
    private long timeWhenPaused;

    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment MainFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainFragment newInstance(String time1) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, time1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        chrono = (Chronometer) view.findViewById(R.id.chronometer1);
        timerState = TimerState.STOPPED;
        //This will need to change once multiple timers are introduced.
    }

    public void startOrPauseTimer() {
        if (timerState == TimerState.STOPPED) { //Start the timer from stopped state
            chrono.setBase(SystemClock.elapsedRealtime());
            chrono.start();
            timerState = TimerState.STARTED;
            Log.d(getClass().getSimpleName(), "Timer started");
        } else if (timerState == TimerState.STARTED) { //Pause the timer from started state
            timeWhenPaused = chrono.getBase() - SystemClock.elapsedRealtime();
            chrono.stop();
            //chrono.setBase(SystemClock.elapsedRealtime());
            timerState = TimerState.PAUSED;
            Log.d(getClass().getSimpleName(), "Timer paused");
        } else if (timerState == TimerState.PAUSED) { //Resume the timer from paused state
            chrono.setBase(SystemClock.elapsedRealtime() + timeWhenPaused);
            chrono.start();
            timerState = TimerState.STARTED;
            Log.d(getClass().getSimpleName(), "Timer started");
        }
    }

    public void resetTimer() {
        chrono.stop();
        chrono.setBase(SystemClock.elapsedRealtime());
        timerState = TimerState.STOPPED;
        Log.d(getClass().getSimpleName(), "Timer reset");
    }

    private enum TimerState {
        STARTED, STOPPED, PAUSED
    }
}
