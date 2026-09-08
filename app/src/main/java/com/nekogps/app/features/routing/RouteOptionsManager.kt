package com.nekogps.app.features.routing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RouteOptionsManager(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nekogps_route_options")
        val AVOID_TOLLS = booleanPreferencesKey("avoid_tolls")
        val AVOID_HIGHWAYS = booleanPreferencesKey("avoid_highways")
        val AVOID_FERRIES = booleanPreferencesKey("avoid_ferries")
        const val DEFAULT_AVOID_TOLLS = false
        const val DEFAULT_AVOID_HIGHWAYS = false
        const val DEFAULT_AVOID_FERRIES = false
    }
    private val TOLL_PENALTY_MULTIPLIER = stringPreferencesKey("toll_penalty_multiplier")
    private val HIGHWAY_PENALTY_MULTIPLIER = stringPreferencesKey("highway_penalty_multiplier")
    private val FERRY_PENALTY_MULTIPLIER = stringPreferencesKey("ferry_penalty_multiplier")

    val avoidTolls: Flow<Boolean> = context.dataStore.data.map { it[AVOID_TOLLS] ?: DEFAULT_AVOID_TOLLS }
    val avoidHighways: Flow<Boolean> = context.dataStore.data.map { it[AVOID_HIGHWAYS] ?: DEFAULT_AVOID_HIGHWAYS }
    val avoidFerries: Flow<Boolean> = context.dataStore.data.map { it[AVOID_FERRIES] ?: DEFAULT_AVOID_FERRIES }
    val tollPenaltyMultiplier: Flow<Double> = context.dataStore.data.map { it[TOLL_PENALTY_MULTIPLIER]?.toDoubleOrNull() ?: 2.0 }
    val highwayPenaltyMultiplier: Flow<Double> = context.dataStore.data.map { it[HIGHWAY_PENALTY_MULTIPLIER]?.toDoubleOrNull() ?: 1.5 }
    val ferryPenaltyMultiplier: Flow<Double> = context.dataStore.data.map { it[FERRY_PENALTY_MULTIPLIER]?.toDoubleOrNull() ?: 3.0 }

    suspend fun setAvoidTolls(avoid: Boolean) { context.dataStore.edit { it[AVOID_TOLLS] = avoid } }
    suspend fun setAvoidHighways(avoid: Boolean) { context.dataStore.edit { it[AVOID_HIGHWAYS] = avoid } }
    suspend fun setAvoidFerries(avoid: Boolean) { context.dataStore.edit { it[AVOID_FERRIES] = avoid } }
    suspend fun setPenaltyMultiplier(type: AvoidanceType, multiplier: Double) {
        context.dataStore.edit { when (type) { AvoidanceType.TOLLS -> it[TOLL_PENALTY_MULTIPLIER] = multiplier.toString(); AvoidanceType.HIGHWAYS -> it[HIGHWAY_PENALTY_MULTIPLIER] = multiplier.toString(); AvoidanceType.FERRIES -> it[FERRY_PENALTY_MULTIPLIER] = multiplier.toString() } }
    }
    suspend fun getPreferences(): RoutePreferences {
        val prefs = context.dataStore.data.first()
        return RoutePreferences(
            avoidTolls = prefs[AVOID_TOLLS] ?: DEFAULT_AVOID_TOLLS,
            avoidHighways = prefs[AVOID_HIGHWAYS] ?: DEFAULT_AVOID_HIGHWAYS,
            avoidFerries = prefs[AVOID_FERRIES] ?: DEFAULT_AVOID_FERRIES,
            tollPenaltyMultiplier = prefs[TOLL_PENALTY_MULTIPLIER]?.toDoubleOrNull() ?: 2.0,
            highwayPenaltyMultiplier = prefs[HIGHWAY_PENALTY_MULTIPLIER]?.toDoubleOrNull() ?: 1.5,
            ferryPenaltyMultiplier = prefs[FERRY_PENALTY_MULTIPLIER]?.toDoubleOrNull() ?: 3.0
        )
    }
    suspend fun resetToDefaults() {
        context.dataStore.edit {
            it[AVOID_TOLLS] = DEFAULT_AVOID_TOLLS
            it[AVOID_HIGHWAYS] = DEFAULT_AVOID_HIGHWAYS
            it[AVOID_FERRIES] = DEFAULT_AVOID_FERRIES
            it[TOLL_PENALTY_MULTIPLIER] = "2.0"
            it[HIGHWAY_PENALTY_MULTIPLIER] = "1.5"
            it[FERRY_PENALTY_MULTIPLIER] = "3.0"
        }
    }
    suspend fun calculatePenalizedDistance(segmentDistance: Double, hasToll: Boolean, hasHighway: Boolean, hasFerry: Boolean): Double {
        val prefs = getPreferences()
        var penalty = 1.0
        if (prefs.avoidTolls && hasToll) penalty *= prefs.tollPenaltyMultiplier
        if (prefs.avoidHighways && hasHighway) penalty *= prefs.highwayPenaltyMultiplier
        if (prefs.avoidFerries && hasFerry) penalty *= prefs.ferryPenaltyMultiplier
        return segmentDistance * penalty
    }
    suspend fun shouldAvoidSegment(routeLabel: String): Boolean {
        val prefs = getPreferences()
        val label = routeLabel.lowercase()
        return when {
            prefs.avoidTolls && (label.contains("toll") || label.contains("highway") && label.contains("toll")) -> true
            prefs.avoidHighways && label.contains("highway") -> true
            prefs.avoidFerries && (label.contains("ferry") || label.contains("bridge")) -> true
            else -> false
        }
    }
}

data class RoutePreferences(
    val avoidTolls: Boolean = false, val avoidHighways: Boolean = false, val avoidFerries: Boolean = false,
    val tollPenaltyMultiplier: Double = 2.0, val highwayPenaltyMultiplier: Double = 1.5, val ferryPenaltyMultiplier: Double = 3.0
) {
    fun getActiveAvoidances(): List<String> {
        val list = mutableListOf<String>()
        if (avoidTolls) list.add("tolls")
        if (avoidHighways) list.add("highways")
        if (avoidFerries) list.add("ferries")
        return list
    }
    fun hasAnyAvoidance(): Boolean = avoidTolls || avoidHighways || avoidFerries
}

enum class AvoidanceType { TOLLS, HIGHWAYS, FERRIES }
