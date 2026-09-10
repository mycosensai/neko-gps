package com.nekogps.app.features.integration

import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Build
import android.view.KeyEvent

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
        } catch (e: Exception) {
            null
        }
    }

    /** Packages with an active media session (requires notification-listener). */
    fun getActivePlayers(): List<PlayerState> {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return emptyList()
            val mgr = sessionManager() ?: return emptyList()
            val component = android.content.ComponentName(context, MusicControlsManager::class.java)
            mgr.getActiveSessions(component).map { c: MediaController ->
                val playing = try {
                    c.playbackState?.state ==
                        android.media.session.PlaybackState.STATE_PLAYING
                } catch (e: Exception) {
                    false
                }
                PlayerState(c.packageName.orEmpty(), playing)
            }
        } catch (se: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
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
        } catch (e: Exception) {
            false
        }
    }
}
