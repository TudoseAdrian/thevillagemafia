package com.example.thevillage

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class NarratorManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("NarratorManager", "Language not supported")
            } else {
                isInitialized = true
                // Try to make it sound a bit deeper and slower for a dramatic effect
                tts?.setPitch(0.8f)
                tts?.setSpeechRate(0.85f)
            }
        } else {
            Log.e("NarratorManager", "Initialization failed")
        }
    }

    fun speak(text: String, flush: Boolean = false) {
        if (isInitialized) {
            val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(text, queueMode, null, null)
        } else {
            Log.w("NarratorManager", "TTS not initialized yet")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
