package com.jarvis.assistant.data.repository

import com.jarvis.assistant.data.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class WeatherRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val _weatherState = MutableStateFlow(
        WeatherData(
            temperature = "28°C",
            condition = "Clear Sky",
            location = "New Delhi, IN",
            humidity = "48%",
            windSpeed = "11 km/h",
            isDay = true,
            lastUpdated = "10:30 AM",
            isLoading = false
        )
    )
    val weatherState: StateFlow<WeatherData> = _weatherState.asStateFlow()

    suspend fun refreshWeather(lat: Double = 28.6139, lon: Double = 77.2090, locationName: String = "Atmosphere Core") {
        _weatherState.value = _weatherState.value.copy(isLoading = true, error = null)

        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,is_day,weather_code,wind_speed_10m"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val root = JSONObject(bodyString)
                    val current = root.getJSONObject("current")

                    val temp = current.getDouble("temperature_2m")
                    val humidity = current.getInt("relative_humidity_2m")
                    val windSpeed = current.getDouble("wind_speed_10m")
                    val isDay = current.getInt("is_day") == 1
                    val weatherCode = current.getInt("weather_code")

                    val conditionDesc = mapWeatherCodeToCondition(weatherCode)
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val updatedTime = timeFormat.format(Date())

                    _weatherState.value = WeatherData(
                        temperature = "${temp.toInt()}°C",
                        condition = conditionDesc,
                        location = locationName,
                        humidity = "$humidity%",
                        windSpeed = "${windSpeed.toInt()} km/h",
                        isDay = isDay,
                        lastUpdated = updatedTime,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _weatherState.value = _weatherState.value.copy(
                        isLoading = false,
                        error = "Atmospheric link unavailable (HTTP ${response.code})"
                    )
                }
            } catch (e: Exception) {
                // Preserves cached live atmosphere with clear network notification
                _weatherState.value = _weatherState.value.copy(
                    isLoading = false,
                    error = "Network offline • Showing cached atmospheric telemetry"
                )
            }
        }
    }

    private fun mapWeatherCodeToCondition(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Atmospheric Fog"
            51, 53, 55 -> "Light Drizzle"
            61, 63, 65 -> "Precipitation Rain"
            71, 73, 75 -> "Light Snowfall"
            80, 81, 82 -> "Rain Showers"
            95, 96, 99 -> "Thunderstorm Alert"
            else -> "Atmospheric Haze"
        }
    }
}
