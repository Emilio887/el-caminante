package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityRecord(
    @PrimaryKey val id: String,
    val title: String,
    val activityType: ActivityType = ActivityType.MTB,
    val startTime: Long,
    val endTime: Long,
    val totalDurationMillis: Long,
    val movingDurationMillis: Long,
    val distanceMeters: Double,
    val avgSpeedKmh: Float,
    val maxSpeedKmh: Float,
    val elevationGain: Double,
    val elevationLoss: Double,
    val minAltitude: Double,
    val maxAltitude: Double,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val avgCadence: Int? = null,
    val avgPower: Int? = null,
    val points: List<GpsPoint> = emptyList(),
    val routeFollowedId: String? = null,
    val notes: String = "",
    val isSynced: Boolean = false
) {
    val distanceKm: Double
        get() = distanceMeters / 1000.0

    val estimatedCalories: Int
        get() = (distanceKm * activityType.calorieFactorPerKm).toInt().coerceAtLeast(10)
}
