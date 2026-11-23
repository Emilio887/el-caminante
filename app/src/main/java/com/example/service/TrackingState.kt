package com.example.service

import com.example.model.ActivityType
import com.example.model.GpsPoint
import com.example.model.Route

enum class RecordingStatus {
    IDLE,
    RECORDING,
    PAUSED
}

data class TrackingState(
    val status: RecordingStatus = RecordingStatus.IDLE,
    val activityType: ActivityType = ActivityType.MTB,
    val currentPoint: GpsPoint? = null,
    val recordedPoints: List<GpsPoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val currentSpeedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val currentAltitude: Double = 0.0,
    val elevationGain: Double = 0.0,
    val elevationLoss: Double = 0.0,
    val elapsedTimeMillis: Long = 0L,
    val movingTimeMillis: Long = 0L,
    val activeReferenceRoute: Route? = null,
    val isOffTrack: Boolean = false,
    val offTrackDistanceMeters: Double = 0.0,
    val offTrackWarningThresholdMeters: Double = 45.0,
    val batteryPercent: Int = 100,
    val isBatteryLowAlert: Boolean = false,
    val keepScreenOn: Boolean = true,
    val headingUpMode: Boolean = false,
    val currentHeading: Float = 0f,
    val currentHeartRate: Int? = null,
    val currentCadence: Int? = null,
    val currentPower: Int? = null,
    val isAutoPauseEnabled: Boolean = true,
    val isAutoPaused: Boolean = false,
    val batterySaverBelow20Enabled: Boolean = true,
    val isBatterySaverActive: Boolean = false
) {
    val distanceKm: Double
        get() = distanceMeters / 1000.0

    val formattedDuration: String
        get() {
            val seconds = (elapsedTimeMillis / 1000) % 60
            val minutes = (elapsedTimeMillis / (1000 * 60)) % 60
            val hours = elapsedTimeMillis / (1000 * 60 * 60)
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}
