package com.jallier.kitchentimer;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "First timer";

    private Chrono[] chronos;

    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param time1 Parameter 1.
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
        //setRetainInstance(true);
        //Log.d(getClass().getSimpleName(), "Fragment created");
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
        chronos = getChronos(view);
    }

    /**
     * Method called when the fragment is rebuilt and views are in place. Allows state to be restored from savedInstance
     *
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        chronos = getChronos(getView());
        if (savedInstanceState != null) {
            int i = 0;
            for (Chrono chrono : chronos) {
                String state = savedInstanceState.getString("Chrono " + i + " State");
                long time = savedInstanceState.getLong("Chrono " + i + " Time");
                chrono.setState(state);
                chrono.setTimeWhenPaused(time);
                chrono.resume();
                i++;
            }
        }
        //Log.d(getClass().getSimpleName(), "instance state restored");
    }

    /**
     * Method that is called when device is rotated. Allows the state of the chronos to be saved to be restored when the fragment is rebuilt
     * @param outState
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        int i = 0;
        for (Chrono chrono : chronos) {
            outState.putString("Chrono " + i + " State", chrono.getState().toString());
            outState.putLong("Chrono " + i + " Time", chrono.getTimeWhenPaused());
            //Log.d(getClass().getSimpleName(), "instance state saved");
            //Log.d(getClass().getSimpleName(), "state is " + chrono.getState().toString());
            i++;
        }
        super.onSaveInstanceState(outState);
    }

    /**
     * Trigger run method of the Chrono that was clicked
     * @param v View of parent activity
     */
    public void startOrPauseTimer(View v) {
        switch (v.getId()) {
            case R.id.chrono0:
                chronos[0].run();
                break;
            case R.id.chrono1:
                chronos[1].run();
                break;
            case R.id.chrono2:
                chronos[2].run();
                break;
            case R.id.chrono3:
                chronos[3].run();
                break;
        }
    }

    /**
     * Reset the timer that was pressed
     * @param v View of parent activity
     */
    public void resetTimer(View v) {
        switch (v.getId()) {
            case R.id.btnReset0:
                chronos[0].reset();
                break;
            case R.id.btnReset1:
                chronos[1].reset();
                break;
            case R.id.btnReset2:
                chronos[2].reset();
                break;
            case R.id.btnReset3:
                chronos[3].reset();
                break;
        }
    }

    /** Get the ids of the Chronos in the main fragment
     * @param v View of parent of Chronos in the main fragment
     * @return Array of Chronometers in the fragment
     */
    protected Chrono[] getChronos(View v) {
        return new Chrono[]{
                (Chrono)v.findViewById(R.id.chrono0),
                (Chrono)v.findViewById(R.id.chrono1),
                (Chrono)v.findViewById(R.id.chrono2),
                (Chrono)v.findViewById(R.id.chrono3)
        };
    }
}
