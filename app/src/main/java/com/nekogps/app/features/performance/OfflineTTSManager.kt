package com.nekogps.app.features.performance

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
class OfflineTTSManager(private val context: Context) : TextToSpeech.OnInitListener {

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

    private var tts: TextToSpeech? = null
    private var ready = false

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
        if (tts == null) tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            applyLanguage(currentLanguageCode)
            listener?.onTtsReady()
        } else {
            listener?.onError("TTS init failed: " + status)
            Log.w(TAG, "TTS init failed: " + status)
        }
    }

    fun isReady(): Boolean = ready

    /** All packs with live installed flags from the TTS engine. */
    fun getVoicePacks(): List<VoicePack> {
        val engine = tts
        return supportedLanguages.map { pack ->
            val avail = try {
                engine?.isLanguageAvailable(pack.locale)
            } catch (e: Exception) { TextToSpeech.LANG_MISSING_DATA }
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
            tts?.setLanguage(pack.locale) ?: TextToSpeech.LANG_MISSING_DATA
        } catch (e: Exception) { TextToSpeech.LANG_MISSING_DATA }
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
    fun downloadVoicePack(languageCode: String): Boolean {
        return try {
            val intent = android.content.Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            listener?.onError("No TTS data installer found")
            false
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH, params: Bundle? = null, utteranceId: String = "neko_tts"): Boolean {
        val engine = tts
        if (!ready || engine == null) return false
        return try {
            engine.speak(text, queueMode, params, utteranceId) == TextToSpeech.SUCCESS
        } catch (e: Exception) {
            Log.w(TAG, "speak failed: " + e.message)
            false
        }
    }

    fun stop() { try { tts?.stop() } catch (e: Exception) { /* ignore */ } }

    fun setSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_RATE, rate).apply()
        try { tts?.setSpeechRate(rate) } catch (e: Exception) { /* ignore */ }
    }

    fun setPitch(pitch: Float) {
        prefs.edit().putFloat(KEY_PITCH, pitch).apply()
        try { tts?.setPitch(pitch) } catch (e: Exception) { /* ignore */ }
    }

    fun getAvailableVoices(): Set<Voice> {
        return try { tts?.voices ?: emptySet() } catch (e: Exception) { emptySet() }
    }

    fun shutdown() {
        try { tts?.shutdown() } catch (e: Exception) { /* ignore */ }
        tts = null
        ready = false
    }

    private fun applyLanguage(languageCode: String) {
        val pack = supportedLanguages.firstOrNull { it.languageCode == languageCode }
            ?: return
        try {
            tts?.setLanguage(pack.locale)
            tts?.setSpeechRate(prefs.getFloat(KEY_RATE, 1.0f))
            tts?.setPitch(prefs.getFloat(KEY_PITCH, 1.0f))
        } catch (e: Exception) { Log.w(TAG, "applyLanguage failed: " + e.message) }
    }

    companion object {
        private const val TAG = "OfflineTTSManager"
        private const val KEY_LANG = "tts_language"
        private const val KEY_RATE = "tts_rate"
        private const val KEY_PITCH = "tts_pitch"
    }
}
