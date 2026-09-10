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
import kotlinx.coroutines.*
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
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

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isExpanded = false
    private var cachedWeatherData: WeatherData? = null
    private var cacheTimestamp: Long = 0
    private val cacheDuration = 30 * 60 * 1000L // 30 minutes

    // API Configuration - users should replace with their own key
    private val apiKey = "YOUR_OPENWEATHERMAP_API_KEY"
    private val baseUrl = "https://api.openweathermap.org/data/2.5"

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
                    fetchCurrentWeather(lat, lon)
                }
                cachedWeatherData = weather
                cacheTimestamp = System.currentTimeMillis()
                updateUI(weather)
            } catch (e: Exception) {
                e.printStackTrace()
                showError()
            }
        }
    }

    private fun fetchCurrentWeather(lat: Double, lon: Double): WeatherData {
        val currentUrl = "$baseUrl/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
        val forecastUrl = "$baseUrl/forecast?lat=$lat&lon=$lon&appid=$apiKey&units=metric"

        val currentJson = makeApiCall(currentUrl)
        val forecastJson = makeApiCall(forecastUrl)

        return parseWeatherData(currentJson, forecastJson)
    }

    private fun makeApiCall(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = reader.readText()
        reader.close()
        connection.disconnect()
        return response
    }

    private fun parseWeatherData(currentJson: String, forecastJson: String): WeatherData {
        val current = JSONObject(currentJson)
        val main = current.getJSONObject("main")
        val wind = current.getJSONObject("wind")
        val weather = current.getJSONArray("weather").getJSONObject(0)

        // Parse forecast
        val forecastObj = JSONObject(forecastJson)
        val forecastList = forecastObj.getJSONArray("list")
        val forecastItems = mutableListOf<ForecastItem>()

        // Get one forecast per day (noon readings)
        val seenDates = mutableSetOf<String>()
        for (i in 0 until forecastList.length()) {
            val item = forecastList.getJSONObject(i)
            val dtTxt = item.getString("dt_txt")
            val date = dtTxt.substring(0, 10)
            if (!seenDates.contains(date) && dtTxt.contains("12:00")) {
                seenDates.add(date)
                val itemMain = item.getJSONObject("main")
                val itemWeather = item.getJSONArray("weather").getJSONObject(0)
                forecastItems.add(
                    ForecastItem(
                        date = date,
                        tempMin = itemMain.getDouble("temp_min"),
                        tempMax = itemMain.getDouble("temp_max"),
                        condition = itemWeather.getString("main"),
                        iconCode = itemWeather.getString("icon")
                    )
                )
                if (forecastItems.size >= 5) break
            }
        }

        return WeatherData(
            temperature = main.getDouble("temp"),
            feelsLike = main.getDouble("feels_like"),
            humidity = main.getInt("humidity"),
            windSpeed = wind.getDouble("speed"),
            condition = weather.getString("main"),
            description = weather.getString("description"),
            iconCode = weather.getString("icon"),
            locationName = current.getString("name"),
            forecast = forecastItems
        )
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
        return try {
            val parts = dateStr.split("-")
            val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val month = months[parts[1].toInt() - 1]
            "$month ${parts[2]}"
        } catch (e: Exception) {
            Log.w("WeatherOverlay", "formatDate: suppressed Exception", e)
            dateStr
        }
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
