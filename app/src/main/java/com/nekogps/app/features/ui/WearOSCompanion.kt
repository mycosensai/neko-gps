package com.nekogps.app.features.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Minimal navigation snapshot shared with a Wear OS companion surface. */
data class WearNavState(
    val nextTurn: String = "",
    val distanceToTurn: String = "",
    val distanceToDestination: String = "",
    val eta: String = "",
    val navigating: Boolean = false
)

/**
 * Phone-only Wear OS helper: keeps the current nav snapshot in memory (and in
 * SharedPreferences for process restarts) and broadcasts it so a Wear app —
 * or any listener — can observe it. No Wear module or wearable dependency needed.
 *
 * Optional delivery via the Wearable Data Layer is attempted through reflection
 * so the app compiles and runs without the wear libraries on the classpath.
 */
class WearOSCompanion(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(loadPersisted())
    val state: StateFlow<WearNavState> = _state.asStateFlow()

    companion object {
        const val PREFS = "wear_companion_prefs"
        const val ACTION_NAV_UPDATE = "com.nekogps.app.features.ui.WEAR_NAV_UPDATE"
        const val EXTRA_NEXT_TURN = "extra_next_turn"
        const val EXTRA_DISTANCE = "extra_distance"
        const val EXTRA_ETA = "extra_eta"
        private const val KEY_NEXT = "next"
        private const val KEY_DIST = "dist"
        private const val KEY_DEST = "dest"
        private const val KEY_ETA = "eta"
        private const val KEY_NAV = "navigating"
    }

    fun publish(state: WearNavState) {
        _state.value = state
        prefs.edit()
            .putString(KEY_NEXT, state.nextTurn)
            .putString(KEY_DIST, state.distanceToTurn)
            .putString(KEY_DEST, state.distanceToDestination)
            .putString(KEY_ETA, state.eta)
            .putBoolean(KEY_NAV, state.navigating)
            .apply()
        appContext.sendBroadcast(
            Intent(ACTION_NAV_UPDATE)
                .putExtra(EXTRA_NEXT_TURN, state.nextTurn)
                .putExtra(EXTRA_DISTANCE, state.distanceToTurn)
                .putExtra(EXTRA_ETA, state.eta)
                .setPackage(appContext.packageName)
        )
        tryDataLayerSync(state)
    }

    fun clear() = publish(WearNavState())

    private fun loadPersisted(): WearNavState = WearNavState(
        nextTurn = prefs.getString(KEY_NEXT, "") ?: "",
        distanceToTurn = prefs.getString(KEY_DIST, "") ?: "",
        distanceToDestination = prefs.getString(KEY_DEST, "") ?: "",
        eta = prefs.getString(KEY_ETA, "") ?: "",
        navigating = prefs.getBoolean(KEY_NAV, false)
    )

    /**
     * Best-effort Data Layer sync via reflection — no-op when the
     * play-services-wearable artifact is absent.
     */
    private fun tryDataLayerSync(state: WearNavState) {
        try {
            val clazz = Class.forName("com.google.android.gms.wearable.Wearable")
            val putMap = Class.forName("com.google.android.gms.wearable.PutDataMapRequest")
            val req = putMap.getMethod("create", String::class.java).invoke(null, "/neko_nav")
            val map = req.javaClass.getMethod("getDataMap").invoke(req)
            map.javaClass.getMethod("putString", String::class.java, String::class.java)
                .invoke(map, "next", state.nextTurn)
            map.javaClass.getMethod("putString", String::class.java, String::class.java)
                .invoke(map, "dist", state.distanceToTurn)
            map.javaClass.getMethod("putString", String::class.java, String::class.java)
                .invoke(map, "eta", state.eta)
            @Suppress("UNCHECKED_CAST")
            val client = clazz.getMethod("getDataClient", Context::class.java).invoke(null, appContext)
            val dataReq = req.javaClass.getMethod("asPutDataRequest").invoke(req)
            client.javaClass.getMethod("putDataItem", Class.forName("com.google.android.gms.wearable.PutDataRequest"))
                .invoke(client, dataReq)
        } catch (e: ReflectiveOperationException) {
            Log.w("WearOSCompanion", "Data Layer sync failed", e)
        } catch (e: SecurityException) {
            Log.w("WearOSCompanion", "Data Layer sync failed", e)
        } catch (e: IllegalArgumentException) {
            Log.w("WearOSCompanion", "Data Layer sync failed", e)
        }
    }
}
