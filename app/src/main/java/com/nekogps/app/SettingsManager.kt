package com.nekogps.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "neko_gps_settings")

/**
 * DataStore-based settings manager for app preferences.
 */
class SettingsManager(private val context: Context) {

    companion object {
        private val KEY_MAP_LAYER = stringPreferencesKey("map_layer")
        private val KEY_GPS_INTERVAL = intPreferencesKey("gps_interval")
        private val KEY_DISTANCE_UNITS = intPreferencesKey("distance_units")
        private val KEY_THEME = intPreferencesKey("theme")
        private val KEY_AUTO_CENTER = booleanPreferencesKey("auto_center")
    }

    // Map Layer: "Standard", "Satellite", "Cycle", "Hiking"
    fun getMapLayer(): Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MAP_LAYER] ?: "Standard"
    }

    suspend fun setMapLayer(layer: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MAP_LAYER] = layer
        }
    }

    // GPS Interval: 0=1s, 1=2s, 2=5s
    fun getGpsInterval(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_GPS_INTERVAL] ?: 0
    }

    suspend fun setGpsInterval(interval: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GPS_INTERVAL] = interval
        }
    }

    // Distance Units: 0=Metric, 1=Imperial
    fun getDistanceUnits(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DISTANCE_UNITS] ?: 0
    }

    suspend fun setDistanceUnits(units: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DISTANCE_UNITS] = units
        }
    }

    // Theme: 0=Dark, 1=Light, 2=System
    fun getTheme(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: 2
    }

    suspend fun setTheme(theme: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme
        }
    }

    // Auto-center map toggle
    fun getAutoCenter(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_CENTER] ?: true
    }

    suspend fun setAutoCenter(autoCenter: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_CENTER] = autoCenter
        }
    }
}
