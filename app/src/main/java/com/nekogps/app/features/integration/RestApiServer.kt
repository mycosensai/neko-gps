package com.nekogps.app.features.integration

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import com.google.gson.Gson
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID
import android.util.Log

/**
 * Tiny localhost REST stub for 3rd-party apps (Tasker, Home Assistant, ...).
 * No embedded HTTP server (keeps the APK lean); instead this class defines the
 * API contract and serves requests through deep-link intents plus a shareable
 * auth token. A real socket server can be dropped in later behind [handleRequest].
 *
 * Endpoints (POST body or query params, header/key "token"):
 *   POST /api/navigate   { destination }      -> opens navigation
 *   GET  /api/location                        -> last known location as JSON
 *   POST /api/waypoint   { lat, lon, name? }  -> broadcast to add a waypoint
 */
class RestApiServer(private val context: Context) {

    companion object {
        const val ACTION_ADD_WAYPOINT = "com.nekogps.app.API_ADD_WAYPOINT"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
        const val EXTRA_NAME = "name"
        private const val PREFS = "rest_api_prefs"
        private const val KEY_TOKEN = "auth_token"
    }

    private val gson = Gson()
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Stable per-install token 3rd-party apps must present. */
    fun getAuthToken(): String {
        var token = prefs.getString(KEY_TOKEN, null)
        if (token.isNullOrBlank()) {
            token = UUID.randomUUID().toString().replace("-", "")
            prefs.edit().putString(KEY_TOKEN, token).apply()
        }
        return token
    }

    fun regenerateToken(): String {
        val token = UUID.randomUUID().toString().replace("-", "")
        prefs.edit().putString(KEY_TOKEN, token).apply()
        return token
    }

    fun isAuthorized(token: String?): Boolean =
        !token.isNullOrBlank() && token == prefs.getString(KEY_TOKEN, null)

    data class ApiResult(val ok: Boolean, val message: String, val data: Any? = null) {
        fun toJson(): String = Gson().toJson(mapOf("ok" to ok, "message" to message, "data" to data))
    }

    /** Route a parsed request; pure logic, safe to unit test. */
    fun handleRequest(path: String, token: String?, params: Map<String, String>): ApiResult {
        if (!isAuthorized(token)) return ApiResult(false, "unauthorized")
        return dispatchAuthorizedRequest(path, params)
    }

    private fun dispatchAuthorizedRequest(path: String, params: Map<String, String>): ApiResult {
        return when (path.trimEnd('/')) {
            "/api/navigate" -> handleNavigateRequest(params)
            "/api/waypoint" -> handleWaypointRequest(params)
            "/api/location" -> {
                ApiResult(true, "ok", mapOf("note" to "location served via fused client"))
            }
            else -> ApiResult(false, "unknown endpoint: $path")
        }
    }

    private fun handleNavigateRequest(params: Map<String, String>): ApiResult {
        val dest = params["destination"].orEmpty()
        if (dest.isBlank()) return ApiResult(false, "missing destination")
        val ok = openNavigation(dest)
        return ApiResult(ok, if (ok) "navigating to $dest" else "failed to navigate")
    }

    private fun handleWaypointRequest(params: Map<String, String>): ApiResult {
        val lat = params["lat"]?.toDoubleOrNull()
        val lon = params["lon"]?.toDoubleOrNull()
        if (lat == null || lon == null) return ApiResult(false, "missing lat/lon")
        context.sendBroadcast(
            Intent(ACTION_ADD_WAYPOINT).apply {
                putExtra(EXTRA_LAT, lat)
                putExtra(EXTRA_LON, lon)
                putExtra(EXTRA_NAME, params["name"].orEmpty())
            }
        )
        return ApiResult(true, "waypoint queued")
    }

    private fun openNavigation(destination: String): Boolean {
        return try {
            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(destination))
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        } catch (e: ActivityNotFoundException) {
            Log.w("RestApiServer", "openNavigation: suppressed Exception", e)
            false
        } catch (e: SecurityException) {
            Log.w("RestApiServer", "openNavigation: suppressed Exception", e)
            false
        }
    }

    fun locationToJson(location: Location?): String {
        if (location == null) return gson.toJson(mapOf("ok" to false))
        return gson.toJson(
            mapOf(
                "ok" to true,
                "lat" to location.latitude,
                "lon" to location.longitude,
                "accuracy" to location.accuracy,
                "time" to location.time
            )
        )
    }

    @Suppress("unused")
    private fun decode(s: String): String = URLDecoder.decode(s, "UTF-8")
}
