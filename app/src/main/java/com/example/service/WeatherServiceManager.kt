package com.example.service

import com.example.model.WeatherInfo
import com.example.model.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getWeatherData(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("hourly") hourly: String = "precipitation_probability"
    ): WeatherResponse
}

object WeatherServiceManager {
    private val client = OkHttpClient.Builder().build()
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val api = retrofit.create(OpenMeteoApi::class.java)

    private val _weatherInfo = MutableStateFlow(WeatherInfo(22.0f, 15, "Agradable", 10.5f))
    val weatherInfo: StateFlow<WeatherInfo> = _weatherInfo.asStateFlow()

    suspend fun fetchWeather(lat: Double, lon: Double) {
        withContext(Dispatchers.IO) {
            try {
                val resp = api.getWeatherData(lat, lon)
                val temp = resp.currentWeather?.temperature ?: 21.0f
                val wind = resp.currentWeather?.windSpeed ?: 10.0f
                val code = resp.currentWeather?.weatherCode ?: 0
                val precip = resp.hourly?.precipitationProbability?.firstOrNull() ?: 10

                val conditionText = when (code) {
                    0 -> "Despejado"
                    in 1..3 -> "Parcialmente Nublado"
                    in 51..67 -> "Lluvia Ligera"
                    in 71..77 -> "Nieve"
                    in 95..99 -> "Tormenta"
                    else -> "Nublado"
                }

                _weatherInfo.value = WeatherInfo(
                    temperatureC = temp,
                    precipitationProbabilityPercent = precip,
                    weatherConditionText = conditionText,
                    windSpeedKmh = wind
                )
            } catch (_: Exception) {
                // Keep fallback mock on network failure
            }
        }
    }
}
