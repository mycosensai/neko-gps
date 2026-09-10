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
        private const val DEFAULT_TOLL_PENALTY_MULTIPLIER = 2.0
        private const val DEFAULT_HIGHWAY_PENALTY_MULTIPLIER = 1.5
        private const val DEFAULT_FERRY_PENALTY_MULTIPLIER = 3.0
    }
    private val tollPenaltyMultiplierKey = stringPreferencesKey("toll_penalty_multiplier")
    private val highwayPenaltyMultiplierKey = stringPreferencesKey("highway_penalty_multiplier")
    private val ferryPenaltyMultiplierKey = stringPreferencesKey("ferry_penalty_multiplier")

    val avoidTolls: Flow<Boolean> = context.dataStore.data.map { it[AVOID_TOLLS] ?: DEFAULT_AVOID_TOLLS }
    val avoidHighways: Flow<Boolean> = context.dataStore.data.map { it[AVOID_HIGHWAYS] ?: DEFAULT_AVOID_HIGHWAYS }
    val avoidFerries: Flow<Boolean> = context.dataStore.data.map { it[AVOID_FERRIES] ?: DEFAULT_AVOID_FERRIES }
    val tollPenaltyMultiplier: Flow<Double> = context.dataStore.data.map {
        it[tollPenaltyMultiplierKey]?.toDoubleOrNull() ?: DEFAULT_TOLL_PENALTY_MULTIPLIER
    }
    val highwayPenaltyMultiplier: Flow<Double> = context.dataStore.data.map {
        it[highwayPenaltyMultiplierKey]?.toDoubleOrNull() ?: DEFAULT_HIGHWAY_PENALTY_MULTIPLIER
    }
    val ferryPenaltyMultiplier: Flow<Double> = context.dataStore.data.map {
        it[ferryPenaltyMultiplierKey]?.toDoubleOrNull() ?: DEFAULT_FERRY_PENALTY_MULTIPLIER
    }

    suspend fun setAvoidTolls(avoid: Boolean) { context.dataStore.edit { it[AVOID_TOLLS] = avoid } }
    suspend fun setAvoidHighways(avoid: Boolean) { context.dataStore.edit { it[AVOID_HIGHWAYS] = avoid } }
    suspend fun setAvoidFerries(avoid: Boolean) { context.dataStore.edit { it[AVOID_FERRIES] = avoid } }
    suspend fun setPenaltyMultiplier(type: AvoidanceType, multiplier: Double) {
        context.dataStore.edit {
            when (type) {
                AvoidanceType.TOLLS -> it[tollPenaltyMultiplierKey] = multiplier.toString()
                AvoidanceType.HIGHWAYS -> it[highwayPenaltyMultiplierKey] = multiplier.toString()
                AvoidanceType.FERRIES -> it[ferryPenaltyMultiplierKey] = multiplier.toString()
            }
        }
    }
    suspend fun getPreferences(): RoutePreferences {
        val prefs = context.dataStore.data.first()
        return RoutePreferences(
            avoidTolls = prefs[AVOID_TOLLS] ?: DEFAULT_AVOID_TOLLS,
            avoidHighways = prefs[AVOID_HIGHWAYS] ?: DEFAULT_AVOID_HIGHWAYS,
            avoidFerries = prefs[AVOID_FERRIES] ?: DEFAULT_AVOID_FERRIES,
            tollPenaltyMultiplier = prefs[tollPenaltyMultiplierKey]?.toDoubleOrNull()
                ?: DEFAULT_TOLL_PENALTY_MULTIPLIER,
            highwayPenaltyMultiplier = prefs[highwayPenaltyMultiplierKey]?.toDoubleOrNull()
                ?: DEFAULT_HIGHWAY_PENALTY_MULTIPLIER,
            ferryPenaltyMultiplier = prefs[ferryPenaltyMultiplierKey]?.toDoubleOrNull()
                ?: DEFAULT_FERRY_PENALTY_MULTIPLIER
        )
    }
    suspend fun resetToDefaults() {
        context.dataStore.edit {
            it[AVOID_TOLLS] = DEFAULT_AVOID_TOLLS
            it[AVOID_HIGHWAYS] = DEFAULT_AVOID_HIGHWAYS
            it[AVOID_FERRIES] = DEFAULT_AVOID_FERRIES
            it[tollPenaltyMultiplierKey] = DEFAULT_TOLL_PENALTY_MULTIPLIER.toString()
            it[highwayPenaltyMultiplierKey] = DEFAULT_HIGHWAY_PENALTY_MULTIPLIER.toString()
            it[ferryPenaltyMultiplierKey] = DEFAULT_FERRY_PENALTY_MULTIPLIER.toString()
        }
    }
    suspend fun calculatePenalizedDistance(
        segmentDistance: Double,
        hasToll: Boolean,
        hasHighway: Boolean,
        hasFerry: Boolean
    ): Double {
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
    val avoidTolls: Boolean = false,
    val avoidHighways: Boolean = false,
    val avoidFerries: Boolean = false,
    val tollPenaltyMultiplier: Double = 2.0,
    val highwayPenaltyMultiplier: Double = 1.5,
    val ferryPenaltyMultiplier: Double = 3.0
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
