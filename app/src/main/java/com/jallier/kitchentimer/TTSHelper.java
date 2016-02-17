package com.jallier.kitchentimer;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

class TTSHelper implements TextToSpeech.OnInitListener {
    private final String LOGTAG = getClass().getSimpleName();
    private TextToSpeech tts;
    private Bundle paramBundle;

    public TTSHelper(Context context) {
        tts = new TextToSpeech(context, this);
        paramBundle = new Bundle();
        paramBundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_NOTIFICATION);
    }

    @Override
    public void onInit(int status) {
        Log.d(LOGTAG, "TTS Initiated");
        tts.setLanguage(Locale.getDefault());
    }

    public void speak(String words) {
        Log.d(LOGTAG, "TTS uttering: " + words);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(words, TextToSpeech.QUEUE_ADD, paramBundle, null);
        } else {
            HashMap<String, String> ttsStream = new HashMap<>();
            ttsStream.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_NOTIFICATION));
            tts.speak(words, TextToSpeech.QUEUE_ADD, ttsStream);
        }
    }

    public void shutdown() {
        Log.d(LOGTAG, "TTS shutting down");
        tts.stop();
        tts.shutdown();
    }
}
