package com.nekogps.app.features.voice

import android.speech.tts.TextToSpeech
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Playback-control foundation for [TextToSpeechService].
 * Owns the engine handle, mute state, and readiness flag so the service stays
 * focused on building navigation announcements.
 */
open class TtsPlaybackController {

    protected var tts: TextToSpeech? = null
    protected val isReady = AtomicBoolean(false)
    protected var isMuted = false
    protected var speakVolume: Float = 1.0f

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
}
