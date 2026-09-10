package com.nekogps.app.features

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.nekogps.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import android.util.Log

/**
 * Weather overlay that fetches data from OpenWeatherMap API (free tier).
 * Shows current conditions as a semi-transparent overlay on MapsActivity.
 * Tap to expand with 5-day forecast. Caches data for 30 minutes.
 */
class WeatherOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isExpanded = false
    private var cachedWeatherData: WeatherData? = null
    private var cacheTimestamp: Long = 0
    private val cacheDuration = CACHE_DURATION_MS // 30 minutes

    // API Configuration - users should replace with their own key
    private val apiKey = "YOUR_OPENWEATHERMAP_API_KEY"
    private val baseUrl = "https://api.openweathermap.org/data/2.5"
    private val apiClient = WeatherApiClient(baseUrl, apiKey)

    // UI Components
    private val cardView: CardView
    private val tvTemperature: TextView
    private val tvCondition: TextView
    private val tvHumidity: TextView
    private val tvWind: TextView
    private val tvLocation: TextView
    private val tvForecast: TextView
    private val forecastContainer: LinearLayout

    data class WeatherData(
        val temperature: Double,
        val feelsLike: Double,
        val humidity: Int,
        val windSpeed: Double,
        val condition: String,
        val description: String,
        val iconCode: String,
        val locationName: String,
        val forecast: List<ForecastItem>
    )

    data class ForecastItem(
        val date: String,
        val tempMin: Double,
        val tempMax: Double,
        val condition: String,
        val iconCode: String
    )

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.TRANSPARENT)

        val view = LayoutInflater.from(context).inflate(R.layout.overlay_weather, this, true)
        cardView = view.findViewById(R.id.cvWeather)
        tvTemperature = view.findViewById(R.id.tvTemperature)
        tvCondition = view.findViewById(R.id.tvCondition)
        tvHumidity = view.findViewById(R.id.tvHumidity)
        tvWind = view.findViewById(R.id.tvWind)
        tvLocation = view.findViewById(R.id.tvLocation)
        tvForecast = view.findViewById(R.id.tvForecast)
        forecastContainer = view.findViewById(R.id.forecastContainer)

        cardView.setOnClickListener {
            toggleExpanded()
        }

        // Initial collapsed state
        collapseView()
    }

    fun updateLocation(location: GeoPoint) {
        val now = System.currentTimeMillis()
        if (cachedWeatherData != null && (now - cacheTimestamp) < cacheDuration) {
            // Use cached data
            updateUI(cachedWeatherData!!)
            return
        }
        fetchWeatherData(location.latitude, location.longitude)
    }

    private fun fetchWeatherData(lat: Double, lon: Double) {
        scope.launch {
            try {
                val weather = withContext(Dispatchers.IO) {
                    apiClient.fetchCurrentWeather(lat, lon)
                }
                cachedWeatherData = weather
                cacheTimestamp = System.currentTimeMillis()
                updateUI(weather)
            } catch (e: IOException) {
                Log.w("WeatherOverlay", "fetchWeatherData failed", e)
                showError()
            } catch (e: JSONException) {
                Log.w("WeatherOverlay", "fetchWeatherData failed", e)
                showError()
            }
        }
    }

    private fun updateUI(data: WeatherData) {
        tvTemperature.text = "${data.temperature.toInt()}°C"
        tvCondition.text = data.description.replaceFirstChar { it.uppercase() }
        tvHumidity.text = "💧 ${data.humidity}%"
        tvWind.text = "💨 ${data.windSpeed} m/s"
        tvLocation.text = data.locationName

        if (isExpanded) {
            buildForecastView(data.forecast)
        }
    }

    private fun buildForecastView(forecast: List<ForecastItem>) {
        forecastContainer.removeAllViews()
        for (item in forecast) {
            val dayView = LayoutInflater.from(context)
                .inflate(R.layout.item_forecast_day, forecastContainer, false)
            val tvDay = dayView.findViewById<TextView>(R.id.tvForecastDay)
            val tvDayTemp = dayView.findViewById<TextView>(R.id.tvForecastTemp)
            val tvDayCond = dayView.findViewById<TextView>(R.id.tvForecastCondition)

            tvDay.text = formatDate(item.date)
            tvDayTemp.text = "${item.tempMin.toInt()}° / ${item.tempMax.toInt()}°"
            tvDayCond.text = item.condition

            forecastContainer.addView(dayView)
        }
    }

    private fun formatDate(dateStr: String): String {
        val parts = dateStr.split("-")
        val monthNum = parts.getOrNull(1)?.toIntOrNull()
        val day = parts.getOrNull(2)
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val month = monthNum?.minus(1)?.let { months.getOrNull(it) }
        if (month != null && day != null) {
            return "$month $day"
        }
        Log.w("WeatherOverlay", "formatDate: unparseable date: $dateStr")
        return dateStr
    }

    private fun showError() {
        tvTemperature.text = "--°C"
        tvCondition.text = "Weather unavailable"
        tvHumidity.text = ""
        tvWind.text = ""
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded
        if (isExpanded) {
            expandView()
        } else {
            collapseView()
        }
    }

    private fun expandView() {
        tvForecast.visibility = View.VISIBLE
        forecastContainer.visibility = View.VISIBLE
        cachedWeatherData?.let { buildForecastView(it.forecast) }
    }

    private fun collapseView() {
        tvForecast.visibility = View.GONE
        forecastContainer.visibility = View.GONE
    }

    fun cleanup() {
        scope.cancel()
    }
}
