package com.nekogps.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-based settings persistence for the Neko GPS app.
 * Stores map layer, GPS interval, distance units, theme, and auto-center preference.
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nekogps_settings")

        val MAP_LAYER = stringPreferencesKey("map_layer")
        val GPS_INTERVAL = longPreferencesKey("gps_interval")
        val DISTANCE_UNITS = stringPreferencesKey("distance_units")
        val THEME = stringPreferencesKey("theme")
        val AUTO_CENTER = booleanPreferencesKey("auto_center")

        private const val DEFAULT_GPS_INTERVAL_MS = 2000L
    }

    // Map Layer Preference
    val mapLayer: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[MAP_LAYER] ?: "Standard"
    }

    suspend fun setMapLayer(layer: String) {
        context.dataStore.edit { preferences ->
            preferences[MAP_LAYER] = layer
        }
    }

    // GPS Update Interval
    val gpsInterval: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[GPS_INTERVAL] ?: DEFAULT_GPS_INTERVAL_MS
    }

    suspend fun setGpsInterval(intervalMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[GPS_INTERVAL] = intervalMs
        }
    }

    // Distance Units
    val distanceUnits: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DISTANCE_UNITS] ?: "Metric"
    }

    suspend fun setDistanceUnits(units: String) {
        context.dataStore.edit { preferences ->
            preferences[DISTANCE_UNITS] = units
        }
    }

    // Theme
    val theme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME] ?: "Dark"
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME] = theme
        }
    }

    // Auto-Center Map
    val autoCenter: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_CENTER] ?: true
    }

    suspend fun setAutoCenter(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CENTER] = enabled
        }
    }

    /**
     * Reset all settings to defaults.
     */
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
