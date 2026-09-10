package com.nekogps.app.features.stats

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * FuelPriceManager - fetches fuel prices from a static JSON dataset.
 */
class FuelPriceManager(private val context: Context) {
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("fuel_prices", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_STATIONS = "station_data"
        private const val KEY_LAST_FETCH = "last_fetch_time"
        private const val CACHE_TTL_MS = 3600_000L
        private const val METERS_PER_KILOMETER = 1000
        private const val LITERS_PER_GALLON = 3.78541
        private const val EARTH_RADIUS_METERS = 6371000.0
    }

    fun getNearbyStations(lat: Double, lon: Double, radiusKm: Double = 20.0): List<FuelStation> {
        val allStations = loadStations()
        return allStations
            .filter { station ->
                val dist = distance(lat, lon, station.latitude, station.longitude)
                dist <= radiusKm * METERS_PER_KILOMETER
            }
            .sortedBy { it.pricePerLiter }
    }

    fun stationsFlow(lat: Double, lon: Double): Flow<List<FuelStation>> = flow {
        emit(getNearbyStations(lat, lon))
    }

    fun formatPrice(station: FuelStation, useMetric: Boolean = true): String {
        val price = station.pricePerLiter
        return if (useMetric) {
            "$%.2f/L".format(price)
        } else {
            "$%.2f/gal".format(price * LITERS_PER_GALLON)
        }
    }

    fun getStationsWithDistance(lat: Double, lon: Double, radiusKm: Double = 20.0): List<FuelStation> {
        return getNearbyStations(lat, lon, radiusKm)
    }

    private fun loadStations(): List<FuelStation> {
        val cached = prefs.getString(KEY_STATIONS, null)
        val lastFetch = prefs.getLong(KEY_LAST_FETCH, 0)
        val now = System.currentTimeMillis()

        if (cached != null && (now - lastFetch) < CACHE_TTL_MS) {
            return gson.fromJson(cached, object : TypeToken<List<FuelStation>>(){}.type)
        }

        val json = context.assets.open("fuel_stations.json").bufferedReader().use { it.readText() }
        val stations: List<FuelStation> = gson.fromJson(json, object : TypeToken<List<FuelStation>>(){}.type)
        prefs.edit().putString(KEY_STATIONS, json).putLong(KEY_LAST_FETCH, now).apply()
        return stations
    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusMeters = EARTH_RADIUS_METERS
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadiusMeters * c
    }
}
