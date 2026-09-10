package com.nekogps.app.features.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import java.util.Locale
import kotlinx.coroutines.launch
import android.widget.TextView

/**
 * Simplified driving UI: oversized touch targets, high-contrast readouts,
 * voice-first interaction (TTS prompts + speech input). Minimal distractions
 * by design — next turn, distance, ETA, and four large actions.
 */
class CarModeActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tvNextTurn: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvEta: TextView
    private lateinit var wearCompanion: WearOSCompanion
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var voiceOnly = true

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager(this).applyStoredTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(com.nekogps.app.R.layout.activity_car_mode)

        wearCompanion = WearOSCompanion(this)
        tts = TextToSpeech(this, this)

        tvNextTurn = findViewById(com.nekogps.app.R.id.tv_car_next)
        tvDistance = findViewById(com.nekogps.app.R.id.tv_car_distance)
        tvEta = findViewById(com.nekogps.app.R.id.tv_car_eta)

        findViewById<MaterialButton>(com.nekogps.app.R.id.btn_car_navigate).setOnClickListener {
            speak(getString(com.nekogps.app.R.string.car_listening))
            startVoiceInput()
        }
        findViewById<MaterialButton>(com.nekogps.app.R.id.btn_car_repeat).setOnClickListener {
            speak("${tvNextTurn.text}. ${tvDistance.text}. ${tvEta.text}")
        }
        findViewById<MaterialButton>(com.nekogps.app.R.id.btn_car_stop).setOnClickListener {
            wearCompanion.clear()
            speak(getString(com.nekogps.app.R.string.car_nav_stopped))
            finish()
        }
        findViewById<MaterialButton>(com.nekogps.app.R.id.btn_car_exit).setOnClickListener { finish() }

        lifecycleScope.launch {
            wearCompanion.state.collect { state ->
                tvNextTurn.text = state.nextTurn.ifEmpty { getString(com.nekogps.app.R.string.widget_no_route) }
                tvDistance.text = state.distanceToTurn.ifEmpty { "--" }
                tvEta.text = state.eta.ifEmpty { "--" }
                if (voiceOnly && state.navigating && state.nextTurn.isNotEmpty()) {
                    speak("${state.nextTurn} in ${state.distanceToTurn}")
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        }
    }

    private fun speak(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "car_mode")
        } else {
            @Suppress("DEPRECATION")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    private fun startVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 41)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak(getString(com.nekogps.app.R.string.car_voice_unavailable))
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (heard.isNotEmpty()) {
                        speak(getString(com.nekogps.app.R.string.car_navigating_to, heard))
                        wearCompanion.publish(
                            WearNavState(nextTurn = heard, navigating = true)
                        )
                    }
                }
                override fun onError(error: Int) = Unit
                override fun onReadyForSpeech(p: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(v: Float) = Unit
                override fun onBufferReceived(b: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(e: Int, p: Bundle?) = Unit
                override fun onPartialResults(p: Bundle?) = Unit
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, getString(com.nekogps.app.R.string.car_listening))
            }
            startListening(intent)
        }
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }
}
