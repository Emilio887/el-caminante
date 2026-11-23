package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class Route(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val activityType: ActivityType = ActivityType.MTB,
    val points: List<GpsPoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val elevationGain: Double = 0.0,
    val elevationLoss: Double = 0.0,
    val difficulty: String = "Moderada", // Fácil, Moderada, Difícil, Muy Difícil
    val authorNickname: String = "Sendero Rider",
    val province: String = "Madrid",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isCommunity: Boolean = false,
    val privacyTrimmed: Boolean = false,
    val isOfflineDownloaded: Boolean = false
) {
    val distanceKm: Double
        get() = distanceMeters / 1000.0

    val estimatedDurationMinutes: Int
        get() {
            val avgSpeedKmH = when (activityType) {
                ActivityType.MTB -> 14.0
                ActivityType.ROAD_CYCLING -> 24.0
                ActivityType.HIKING -> 4.0
                ActivityType.RUNNING -> 10.0
                ActivityType.WALKING -> 4.5
            }
            val baseHours = (distanceMeters / 1000.0) / avgSpeedKmH
            // Add elevation factor (1 hour per 500m climbing)
            val climbHours = elevationGain / 500.0
            return ((baseHours + climbHours) * 60).toInt().coerceAtLeast(1)
        }
}
