package com.jallier.kitchentimer;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TTSHelper implements TextToSpeech.OnInitListener {
    private final String LOGTAG = getClass().getSimpleName();
    private TextToSpeech tts;

    public TTSHelper(Context context) {
        tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        Log.d(LOGTAG, "TTS Initiated");
        tts.setLanguage(Locale.getDefault());
    }

    public void speak(String words) {
        Log.d(LOGTAG, "TTS uttering");
        tts.speak(words, TextToSpeech.QUEUE_ADD, null, null);
    }

    public void shutdown() {
        Log.d(LOGTAG, "TTS shutting down");
        tts.stop();
        tts.shutdown();
    }
}
