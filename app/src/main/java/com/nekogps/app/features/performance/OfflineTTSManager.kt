package com.nekogps.app.features.performance

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Phase 10: Download and manage offline TTS voice packs.
 * Wraps platform TextToSpeech; tracks installed voices per language
 * and fires the system TTS-data installer for missing languages.
 */
/**
 * Engine lifecycle owned by the TTS manager: init state, stop/shutdown and
 * voice enumeration. Split out so OfflineTTSManager stays under the
 * function-count budget; all members remain reachable through it.
 */
open class TtsEngineBasics {

    protected var engine: TextToSpeech? = null
    protected var engineReady: Boolean = false

    fun isReady(): Boolean = engineReady

    fun stop() { try { engine?.stop() } catch (e: IllegalStateException) {
        Log.w("OfflineTTSManager", "stop: suppressed Exception", e)
        /* ignore */ } }

    fun getAvailableVoices(): Set<Voice> {
        return try { engine?.voices ?: emptySet() } catch (e: IllegalStateException) {
            Log.w("OfflineTTSManager", "getAvailableVoices: suppressed Exception", e)
            emptySet() }
    }

    fun shutdown() {
        try { engine?.shutdown() } catch (e: IllegalStateException) {
            Log.w("OfflineTTSManager", "shutdown: suppressed Exception", e)
            /* ignore */ }
        engine = null
        engineReady = false
    }
}

class OfflineTTSManager(private val context: Context) :
    TtsEngineBasics(),
    TextToSpeech.OnInitListener {

    data class VoicePack(
        val languageCode: String,
        val displayName: String,
        val locale: Locale,
        var installed: Boolean = false
    )

    interface Listener {
        fun onTtsReady() {}
        fun onLanguageChanged(languageCode: String) {}
        fun onError(message: String) {}
    }

    var listener: Listener? = null

    private val prefs: SharedPreferences =
        context.getSharedPreferences("offline_tts_prefs", Context.MODE_PRIVATE)

    val supportedLanguages: List<VoicePack> = listOf(
        VoicePack("en-US", "English (US)", Locale.US),
        VoicePack("en-GB", "English (UK)", Locale.UK),
        VoicePack("es-ES", "Spanish", Locale("es", "ES")),
        VoicePack("fr-FR", "French", Locale.FRANCE),
        VoicePack("de-DE", "German", Locale.GERMANY),
        VoicePack("ja-JP", "Japanese", Locale.JAPAN)
    )

    var currentLanguageCode: String
        get() = prefs.getString(KEY_LANG, Locale.getDefault().toLanguageTag() ?: "en-US") ?: "en-US"
        private set(v) { prefs.edit().putString(KEY_LANG, v).apply() }

    fun init() {
        if (engine == null) {
            engine = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        engineReady = status == TextToSpeech.SUCCESS
        if (engineReady) {
            applyLanguage(currentLanguageCode)
            listener?.onTtsReady()
        } else {
            listener?.onError("TTS init failed: " + status)
            Log.w(TAG, "TTS init failed: " + status)
        }
    }

    /** All packs with live installed flags from the TTS engine. */
    fun getVoicePacks(): List<VoicePack> {
        return supportedLanguages.map { pack ->
            val avail = try {
                engine?.isLanguageAvailable(pack.locale)
            } catch (e: IllegalArgumentException) {
                Log.w("OfflineTTSManager", "getVoicePacks: suppressed Exception", e)
                TextToSpeech.LANG_MISSING_DATA } catch (e: IllegalStateException) {
                Log.w("OfflineTTSManager", "getVoicePacks: suppressed Exception", e)
                TextToSpeech.LANG_MISSING_DATA }
            pack.copy(
                installed = avail == TextToSpeech.LANG_AVAILABLE ||
                    avail == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    avail == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            )
        }
    }

    fun isInstalled(languageCode: String): Boolean =
        getVoicePacks().firstOrNull { it.languageCode == languageCode }?.installed ?: false

    /** Select a language; returns true if usable offline, false if install needed. */
    fun selectLanguage(languageCode: String): Boolean {
        val pack = supportedLanguages.firstOrNull { it.languageCode == languageCode }
            ?: return false
        val res = try {
            engine?.setLanguage(pack.locale) ?: TextToSpeech.LANG_MISSING_DATA
        } catch (e: IllegalArgumentException) {
            Log.w("OfflineTTSManager", "selectLanguage: suppressed Exception", e)
            TextToSpeech.LANG_MISSING_DATA } catch (e: IllegalStateException) {
            Log.w("OfflineTTSManager", "selectLanguage: suppressed Exception", e)
            TextToSpeech.LANG_MISSING_DATA }
        val ok = res == TextToSpeech.LANG_AVAILABLE ||
            res == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            res == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        if (ok) {
            currentLanguageCode = languageCode
            listener?.onLanguageChanged(languageCode)
        }
        return ok
    }

    /** Open the system installer to download TTS voice data for a language. */
    fun downloadVoicePack(): Boolean {
        return try {
            val intent = android.content.Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w("OfflineTTSManager", "downloadVoicePack: suppressed Exception", e)
            listener?.onError("No TTS data installer found")
            false
        } catch (e: SecurityException) {
            Log.w("OfflineTTSManager", "downloadVoicePack: suppressed Exception", e)
            listener?.onError("No TTS data installer found")
            false
        }
    }

    fun speak(
        text: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        params: Bundle? = null,
        utteranceId: String = DEFAULT_UTTERANCE_ID
    ): Boolean {
        val current = engine
        if (!engineReady || current == null) return false
        return try {
            current.speak(text, queueMode, params, utteranceId) == TextToSpeech.SUCCESS
        } catch (e: IllegalStateException) {
            Log.w("OfflineTTSManager", "speak: suppressed Exception", e)
            Log.w(TAG, "speak failed", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.w("OfflineTTSManager", "speak: suppressed Exception", e)
            Log.w(TAG, "speak failed", e)
            false
        }
    }

    fun setSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_RATE, rate).apply()
        try { engine?.setSpeechRate(rate) } catch (e: IllegalStateException) {
            Log.w("OfflineTTSManager", "setSpeechRate: suppressed Exception", e)
            /* ignore */ }
    }

    fun setPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_PITCH, pitch).apply()
        try { engine?.setPitch(pitch) } catch (e: IllegalStateException) {
            Log.w("OfflineTTSManager", "setPitch: suppressed Exception", e)
            /* ignore */ }
    }

    private fun applyLanguage(languageCode: String) {
        val pack = supportedLanguages.firstOrNull { it.languageCode == languageCode }
            ?: return
        try {
            engine?.setLanguage(pack.locale)
            engine?.setSpeechRate(prefs.getFloat(KEY_RATE, DEFAULT_SPEECH_RATE))
            engine?.setPitch(prefs.getFloat(KEY_PITCH, DEFAULT_SPEECH_PITCH))
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "applyLanguage failed", e)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "applyLanguage failed", e)
        }
    }

    companion object {
        private const val TAG = "OfflineTTSManager"
        private const val KEY_LANG = "tts_language"
        private const val KEY_RATE = "tts_rate"
        private const val KEY_PITCH = "tts_pitch"
        private const val DEFAULT_UTTERANCE_ID = "neko_tts"
        private const val DEFAULT_SPEECH_RATE = 1.0f
        private const val DEFAULT_SPEECH_PITCH = 1.0f
    }
}
