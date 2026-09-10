package com.nekogps.app.features.integration

import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Build
import android.view.KeyEvent
import android.util.Log

/**
 * Media controls for music apps (Spotify, YouTube Music, etc.) shown in the
 * navigation HUD. Uses the platform MediaSessionManager on API 21+; every
 * call is guarded so missing permissions or players simply report false.
 */
class MusicControlsManager(private val context: Context) {

    data class PlayerState(
        val packageName: String,
        val playing: Boolean
    )

    private fun sessionManager(): MediaSessionManager? {
        return try {
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        } catch (e: SecurityException) {
            Log.w("MusicControlsManager", "sessionManager: suppressed Exception", e)
            null
        } catch (e: IllegalStateException) {
            Log.w("MusicControlsManager", "sessionManager: suppressed Exception", e)
            null
        }
    }

    /** Packages with an active media session (requires notification-listener). */
    fun getActivePlayers(): List<PlayerState> {
        return runCatching { queryActivePlayers() }
            .onFailure { Log.w("MusicControlsManager", "getActivePlayers: failed", it) }
            .getOrDefault(emptyList())
    }

    private fun queryActivePlayers(): List<PlayerState> {
        val mgr = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) sessionManager() else null)
            ?: return emptyList()
            val component = android.content.ComponentName(context, MusicControlsManager::class.java)
            return mgr.getActiveSessions(component).map { c: MediaController ->
                val playing = try {
                    c.playbackState?.state ==
                        android.media.session.PlaybackState.STATE_PLAYING
                } catch (e: IllegalStateException) {
                    Log.w("MusicControlsManager", "getActivePlayers: suppressed Exception", e)
                    false
                }
                PlayerState(c.packageName.orEmpty(), playing)
            }
    }

    fun dispatchPlayPause(): Boolean = dispatchKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    fun dispatchNext(): Boolean = dispatchKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    fun dispatchPrevious(): Boolean = dispatchKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    private fun dispatchKey(keyCode: Int): Boolean {
        return try {
            val down = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val up = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, down)
            }
            context.sendBroadcast(intent)
            val intentUp = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, up)
            }
            context.sendBroadcast(intentUp)
            true
        } catch (e: SecurityException) {
            Log.w("MusicControlsManager", "dispatchKey: suppressed Exception", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.w("MusicControlsManager", "dispatchKey: suppressed Exception", e)
            false
        }
    }
}
