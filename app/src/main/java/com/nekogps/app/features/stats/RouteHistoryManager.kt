package com.nekogps.app.features.stats

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * RouteHistoryManager - loads saved tracks from SharedPreferences.
 */
class RouteHistoryManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("route_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_ROUTES = "saved_routes"
        private const val KEY_COUNT = "route_count"
    }

    fun saveRoute(routeName: String, points: List<RoutePoint>) {
        val json = gson.toJson(points)
        prefs.edit().putString(KEY_ROUTES + "_" + routeName, json).apply()
        val count = prefs.getInt(KEY_COUNT, 0) + 1
        prefs.edit().putInt(KEY_COUNT, count).apply()
    }

    fun getRoute(routeName: String): List<RoutePoint>? {
        val json = prefs.getString(KEY_ROUTES + "_" + routeName, null) ?: return null
        val type = object : TypeToken<List<RoutePoint>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getAllRouteNames(): List<String> {
        val keys = prefs.all.keys.filter { it.startsWith(KEY_ROUTES) && !it.contains("_count") }
        return keys.map { it.removePrefix(KEY_ROUTES + "_") }
    }

    fun routesFlow(): Flow<List<RoutePoint>> = flow {
        val names = getAllRouteNames()
        names.forEach { name ->
            val points = getRoute(name)
            if (!points.isNullOrEmpty()) {
                emit(points)
            }
        }
    }

    fun deleteRoute(routeName: String) {
        prefs.edit().remove(KEY_ROUTES + "_" + routeName).apply()
    }

    fun getRouteCount(): Int = prefs.getInt(KEY_COUNT, 0)

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun formatDistance(meters: Float): String {
        return if (meters >= 1000) "%.1f km".format(meters / 1000) else "%.0f m".format(meters)
    }
}
