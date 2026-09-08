package com.nekogps.app.features.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service for announcing turn-by-turn navigation instructions using Android's TextToSpeech.
 * Supports speaking directions, speed limits, and alerts.
 */
class TextToSpeechService(context: Context) {

    private var tts: TextToSpeech? = null
    private val isReady = AtomicBoolean(false)
    private var speakVolume: Float = 1.0f
    private var speechRate: Float = 1.0f
    private var isMuted = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(speechRate)
                isReady.set(true)
            }
        }
    }

    /**
     * Set speech rate (0.5 = slow, 1.0 = normal, 2.0 = fast)
     */
    fun setSpeechRate(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
    }

    /**
     * Speak a navigation instruction.
     */
    fun speakInstruction(instruction: String) {
        if (isMuted || !isReady.get()) return
        speak(instruction, "instruction_${System.currentTimeMillis()}")
    }

    /**
     * Speak a speed limit warning.
     */
    fun speakSpeedLimit(speedLimitKmh: Int) {
        if (isMuted || !isReady.get()) return
        speak("Speed limit $speedLimitKmh kilometers per hour", "speedlimit_$speedLimitKmh")
    }

    /**
     * Speak a speed camera alert.
     */
    fun speakSpeedCameraAlert(distanceMeters: Int) {
        if (isMuted || !isReady.get()) return
        val distanceText = when {
            distanceMeters < 100 -> "in $distanceMeters meters"
            else -> "in ${distanceMeters / 100 * 100} meters"
        }
        speak("Warning! Speed camera ahead $distanceText", "camera_alert_${System.currentTimeMillis()}")
    }

    /**
     * Speak a general alert message.
     */
    fun speakAlert(message: String) {
        if (isMuted || !isReady.get()) return
        speak("Alert: $message", "alert_${System.currentTimeMillis()}")
    }

    /**
     * Speak arrival message.
     */
    fun speakArrival() {
        if (isMuted || !isReady.get()) return
        speak("You have arrived at your destination", "arrival")
    }

    /**
     * Speak current speed.
     */
    fun speakSpeed(speedKmh: Int) {
        if (isMuted || !isReady.get()) return
        speak("$speedKmh kilometers per hour", "speed_${System.currentTimeMillis()}")
    }

    /**
     * Toggle mute state.
     */
    fun toggleMute(): Boolean {
        isMuted = !isMuted
        if (isMuted) {
            tts?.stop()
        }
        return isMuted
    }

    fun isMuted(): Boolean = isMuted

    /**
     * Stop current speech.
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * Check if TTS is ready to speak.
     */
    fun isReady(): Boolean = isReady.get()

    /**
     * Shutdown the TTS engine. Call when done with the service.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady.set(false)
    }

    private fun speak(text: String, utteranceId: String) {
        if (!isReady.get()) return
        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, speakVolume)
        }
        tts?.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
    }
}
