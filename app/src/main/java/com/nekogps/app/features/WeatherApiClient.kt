package com.nekogps.app.features

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * HTTP client for OpenWeatherMap current + forecast data.
 * Extracted from [WeatherOverlay] so the view stays under the function-count limit.
 */
class WeatherApiClient(
    private val baseUrl: String,
    private val apiKey: String
) {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
        private const val MAX_FORECAST_DAYS = 5
        private const val DATE_PREFIX_LENGTH = 10
    }

    fun fetchCurrentWeather(lat: Double, lon: Double): WeatherOverlay.WeatherData {
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
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = reader.readText()
        reader.close()
        connection.disconnect()
        return response
    }

    private fun parseWeatherData(
        currentJson: String,
        forecastJson: String
    ): WeatherOverlay.WeatherData {
        val current = JSONObject(currentJson)
        val main = current.getJSONObject("main")
        val wind = current.getJSONObject("wind")
        val weather = current.getJSONArray("weather").getJSONObject(0)

        // Parse forecast
        val forecastObj = JSONObject(forecastJson)
        val forecastList = forecastObj.getJSONArray("list")
        val forecastItems = mutableListOf<WeatherOverlay.ForecastItem>()

        // Get one forecast per day (noon readings)
        val seenDates = mutableSetOf<String>()
        for (i in 0 until forecastList.length()) {
            val item = forecastList.getJSONObject(i)
            val dtTxt = item.getString("dt_txt")
            val date = dtTxt.substring(0, DATE_PREFIX_LENGTH)
            if (!seenDates.contains(date) && dtTxt.contains("12:00")) {
                seenDates.add(date)
                val itemMain = item.getJSONObject("main")
                val itemWeather = item.getJSONArray("weather").getJSONObject(0)
                forecastItems.add(
                    WeatherOverlay.ForecastItem(
                        date = date,
                        tempMin = itemMain.getDouble("temp_min"),
                        tempMax = itemMain.getDouble("temp_max"),
                        condition = itemWeather.getString("main"),
                        iconCode = itemWeather.getString("icon")
                    )
                )
                if (forecastItems.size >= MAX_FORECAST_DAYS) break
            }
        }

        return WeatherOverlay.WeatherData(
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
}
