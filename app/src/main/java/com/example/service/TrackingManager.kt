package com.example.service

import android.content.Context
import android.os.BatteryManager
import com.example.gps.OffTrackCalculator
import com.example.model.ActivityRecord
import com.example.model.ActivityType
import com.example.model.GpsPoint
import com.example.model.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

object TrackingManager {

    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var sessionStartTime: Long = 0L
    private var lastRecordedPoint: GpsPoint? = null
    private var stationaryCount = 0

    fun setAutoPauseEnabled(enabled: Boolean) {
        _state.update { it.copy(isAutoPauseEnabled = enabled) }
    }

    fun setBatterySaverBelow20Enabled(enabled: Boolean) {
        _state.update { it.copy(batterySaverBelow20Enabled = enabled) }
    }

    fun setActivityType(type: ActivityType) {
        _state.update { it.copy(activityType = type) }
    }

    fun setReferenceRoute(route: Route?) {
        _state.update { it.copy(activeReferenceRoute = route) }
    }

    fun setKeepScreenOn(keepOn: Boolean) {
        _state.update { it.copy(keepScreenOn = keepOn) }
    }

    fun setHeadingUpMode(headingUp: Boolean) {
        _state.update { it.copy(headingUpMode = headingUp) }
    }

    fun updateSensorData(heartRate: Int?, cadence: Int?, power: Int?) {
        _state.update {
            it.copy(
                currentHeartRate = heartRate ?: it.currentHeartRate,
                currentCadence = cadence ?: it.currentCadence,
                currentPower = power ?: it.currentPower
            )
        }
    }

    fun checkBattery(context: Context) {
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 80
            val isLow = level <= 20
            val saverActive = isLow && _state.value.batterySaverBelow20Enabled
            if (saverActive) {
                com.example.sensor.BleSensorManager.stopScan()
            }
            _state.update {
                it.copy(
                    batteryPercent = level,
                    isBatteryLowAlert = isLow,
                    isBatterySaverActive = saverActive
                )
            }
        } catch (_: Exception) {}
    }

    fun dismissBatteryAlert() {
        _state.update { it.copy(isBatteryLowAlert = false) }
    }

    fun startRecording(context: Context) {
        if (_state.value.status == RecordingStatus.RECORDING) return
        sessionStartTime = System.currentTimeMillis()
        _state.update {
            it.copy(
                status = RecordingStatus.RECORDING,
                distanceMeters = 0.0,
                elevationGain = 0.0,
                elevationLoss = 0.0,
                elapsedTimeMillis = 0L,
                movingTimeMillis = 0L,
                recordedPoints = emptyList(),
                currentSpeedKmh = 0f,
                avgSpeedKmh = 0f,
                maxSpeedKmh = 0f
            )
        }
        checkBattery(context)
        startTimer()
    }

    fun pauseRecording() {
        _state.update { it.copy(status = RecordingStatus.PAUSED, isAutoPaused = false) }
        timerJob?.cancel()
    }

    fun resumeRecording() {
        if (_state.value.status != RecordingStatus.PAUSED) return
        _state.update { it.copy(status = RecordingStatus.RECORDING, isAutoPaused = false) }
        stationaryCount = 0
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _state.value.status == RecordingStatus.RECORDING) {
                delay(1000L)
                _state.update { curr ->
                    val newElapsed = curr.elapsedTimeMillis + 1000L
                    val moving = if (curr.currentSpeedKmh > 1.5f) curr.movingTimeMillis + 1000L else curr.movingTimeMillis
                    val avg = if (newElapsed > 0) ((curr.distanceMeters / 1000.0) / (newElapsed / 3600000.0)).toFloat() else 0f
                    curr.copy(
                        elapsedTimeMillis = newElapsed,
                        movingTimeMillis = moving,
                        avgSpeedKmh = avg
                    )
                }
            }
        }
    }

    fun onLocationUpdate(rawPoint: GpsPoint) {
        val currentPoint = rawPoint.copy(
            heartRate = _state.value.currentHeartRate,
            cadence = _state.value.currentCadence,
            power = _state.value.currentPower
        )

        _state.update { curr ->
            val prev = lastRecordedPoint
            var addDist = 0.0
            var gain = curr.elevationGain
            var loss = curr.elevationLoss
            val updatedPoints = curr.recordedPoints.toMutableList()

            if (curr.status == RecordingStatus.RECORDING) {
                if (prev != null) {
                    val stepDist = prev.distanceTo(currentPoint)
                    // Discard GPS jitter below 2 meters or unrealistic teleportation (> 100m in 1s)
                    if (stepDist in 2.0..100.0) {
                        addDist = stepDist
                        val dEle = currentPoint.altitude - prev.altitude
                        if (dEle > 0.8) gain += dEle
                        else if (dEle < -0.8) loss += -dEle
                        updatedPoints.add(currentPoint)
                        lastRecordedPoint = currentPoint
                    }
                } else {
                    updatedPoints.add(currentPoint)
                    lastRecordedPoint = currentPoint
                }
            }

            // Check off-track alert if reference route is active
            var isOff = false
            var offDist = 0.0
            curr.activeReferenceRoute?.let { ref ->
                if (ref.points.isNotEmpty()) {
                    offDist = OffTrackCalculator.getDistanceToRoute(currentPoint, ref.points)
                    isOff = offDist > curr.offTrackWarningThresholdMeters
                }
            }

            val newSpeed = currentPoint.speedKmh
            val maxSpd = maxOf(curr.maxSpeedKmh, newSpeed)
            val cadence = currentPoint.cadence ?: 0

            // Auto-pause and auto-resume using GPS speed and motion sensor data
            if (curr.isAutoPauseEnabled) {
                if (curr.status == RecordingStatus.RECORDING) {
                    if (newSpeed < 1.2f && cadence <= 2) {
                        stationaryCount++
                        if (stationaryCount >= 4) {
                            _state.update { it.copy(status = RecordingStatus.PAUSED, isAutoPaused = true) }
                            timerJob?.cancel()
                        }
                    } else if (newSpeed >= 1.8f || cadence > 5) {
                        stationaryCount = 0
                    }
                } else if (curr.status == RecordingStatus.PAUSED && curr.isAutoPaused) {
                    if (newSpeed >= 1.8f || cadence > 5) {
                        _state.update { it.copy(status = RecordingStatus.RECORDING, isAutoPaused = false) }
                        stationaryCount = 0
                        startTimer()
                    }
                }
            }

            curr.copy(
                currentPoint = currentPoint,
                recordedPoints = updatedPoints,
                distanceMeters = curr.distanceMeters + addDist,
                currentSpeedKmh = newSpeed,
                maxSpeedKmh = maxSpd,
                currentAltitude = currentPoint.altitude,
                elevationGain = gain,
                elevationLoss = loss,
                currentHeading = currentPoint.bearing,
                isOffTrack = isOff,
                offTrackDistanceMeters = offDist
            )
        }
    }

    fun finishRecording(notes: String = ""): ActivityRecord? {
        val s = _state.value
        timerJob?.cancel()
        if (s.recordedPoints.isEmpty() && s.distanceMeters < 50.0) {
            _state.update { it.copy(status = RecordingStatus.IDLE) }
            return null
        }

        val minAlt = s.recordedPoints.minOfOrNull { it.altitude } ?: s.currentAltitude
        val maxAlt = s.recordedPoints.maxOfOrNull { it.altitude } ?: s.currentAltitude

        val record = ActivityRecord(
            id = UUID.randomUUID().toString(),
            title = "${s.activityType.labelEs} - Sesión Sendero",
            activityType = s.activityType,
            startTime = sessionStartTime,
            endTime = System.currentTimeMillis(),
            totalDurationMillis = s.elapsedTimeMillis,
            movingDurationMillis = s.movingTimeMillis,
            distanceMeters = s.distanceMeters,
            avgSpeedKmh = s.avgSpeedKmh,
            maxSpeedKmh = s.maxSpeedKmh,
            elevationGain = s.elevationGain,
            elevationLoss = s.elevationLoss,
            minAltitude = minAlt,
            maxAltitude = maxAlt,
            avgHeartRate = s.currentHeartRate,
            maxHeartRate = s.currentHeartRate,
            avgCadence = s.currentCadence,
            avgPower = s.currentPower,
            points = s.recordedPoints,
            routeFollowedId = s.activeReferenceRoute?.id,
            notes = notes
        )

        _state.update {
            it.copy(
                status = RecordingStatus.IDLE,
                recordedPoints = emptyList(),
                distanceMeters = 0.0,
                elapsedTimeMillis = 0L
            )
        }
        lastRecordedPoint = null
        return record
    }

    fun discardRecording() {
        timerJob?.cancel()
        lastRecordedPoint = null
        _state.update {
            it.copy(
                status = RecordingStatus.IDLE,
                recordedPoints = emptyList(),
                distanceMeters = 0.0,
                elapsedTimeMillis = 0L,
                currentSpeedKmh = 0f
            )
        }
    }
}
