package com.nekogps.app.features.integration

import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale

/** A parsed voice command: action plus optional argument. */
data class VoiceCommand(
    val action: VoiceAction,
    val argument: String = ""
)

enum class VoiceAction {
    NAVIGATE_HOME,
    NAVIGATE_TO,
    FIND_GAS,
    FIND_FOOD,
    REPORT_SPEED_CAMERA,
    SHARE_LOCATION,
    STOP_NAVIGATION,
    UNKNOWN
}

/**
 * Custom voice commands ("navigate home", "find gas", "report speed camera").
 * Recognition itself uses the platform speech-recognizer intent; parsing is a
 * small on-device matcher so no network or extra dependency is required.
 */
class VoiceAssistantManager {

    /** Intent the caller can launch with startActivityForResult. */
    fun buildRecognitionIntent(prompt: String = "Say a command"): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
    }

    fun parseCommand(text: String): VoiceCommand {
        val t = text.lowercase(Locale.US).trim()
        return when {
            t.contains("navigate home") || t == "go home" -> VoiceCommand(VoiceAction.NAVIGATE_HOME)
            t.startsWith("navigate to ") || t.startsWith("go to ") || t.startsWith("drive to ") ->
                VoiceCommand(VoiceAction.NAVIGATE_TO, text.substringAfter("to ").trim())
            t.contains("gas") || t.contains("fuel") || t.contains("petrol") ->
                VoiceCommand(VoiceAction.FIND_GAS)
            t.contains("food") || t.contains("restaurant") || t.contains("eat") ->
                VoiceCommand(VoiceAction.FIND_FOOD)
            t.contains("speed camera") || t.contains("speed trap") ->
                VoiceCommand(VoiceAction.REPORT_SPEED_CAMERA)
            t.contains("share") && t.contains("location") ->
                VoiceCommand(VoiceAction.SHARE_LOCATION)
            t.contains("stop navigation") || t.contains("cancel navigation") || t.contains("stop navigating") ->
                VoiceCommand(VoiceAction.STOP_NAVIGATION)
            else -> VoiceCommand(VoiceAction.UNKNOWN, text)
        }
    }

    /** First result of a recognizer round-trip, parsed into a command. */
    fun parseRecognizerResults(results: ArrayList<String>?): VoiceCommand {
        val best = results?.firstOrNull().orEmpty()
        if (best.isBlank()) return VoiceCommand(VoiceAction.UNKNOWN)
        return parseCommand(best)
    }
}
