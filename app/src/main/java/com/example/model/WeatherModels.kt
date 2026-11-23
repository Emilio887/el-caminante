package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "current_weather") val currentWeather: CurrentWeather?,
    @Json(name = "hourly") val hourly: HourlyWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "temperature") val temperature: Float,
    @Json(name = "windspeed") val windSpeed: Float,
    @Json(name = "weathercode") val weatherCode: Int
)

@JsonClass(generateAdapter = true)
data class HourlyWeather(
    @Json(name = "time") val time: List<String>,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>?
)

data class WeatherInfo(
    val temperatureC: Float = 21.5f,
    val precipitationProbabilityPercent: Int = 10,
    val weatherConditionText: String = "Despejado",
    val windSpeedKmh: Float = 12.0f
)
