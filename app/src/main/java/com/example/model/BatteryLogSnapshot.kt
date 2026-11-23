package com.example.model

data class BatteryLogSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val batteryLevelPercent: Int,
    val temperatureCelsius: Float,
    val voltageMilliVolts: Int,
    val isCharging: Boolean,
    val screenStateOn: Boolean = true,
    val activityDurationMinutes: Int = 0
)

data class BatteryOptimizationTip(
    val id: String,
    val title: String,
    val description: String,
    val estimatedSavingsPercent: Int,
    val isActionable: Boolean = true,
    val actionType: String = "" // ECO_GPS, AUTO_SCREEN_OFF, DARK_MODE, HIGH_TEMP
)

data class BatteryUsageSummary(
    val startLevelPercent: Int = 100,
    val currentLevelPercent: Int = 100,
    val totalDropPercent: Int = 0,
    val elapsedDurationMinutes: Int = 0,
    val drainRatePerHour: Float = 0f, // e.g. 7.5% per hour
    val estimatedRemainingHours: Float = 0f,
    val averageTemperatureCelsius: Float = 25f,
    val maxTemperatureCelsius: Float = 25f,
    val isEcoModeEnabled: Boolean = false,
    val snapshots: List<BatteryLogSnapshot> = emptyList(),
    val optimizationTips: List<BatteryOptimizationTip> = emptyList()
) {
    val formattedRemainingTime: String
        get() {
            if (drainRatePerHour <= 0.5f) return "> 12 horas"
            val totalMinutes = (estimatedRemainingHours * 60).toInt()
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }

    val formattedDrainRate: String
        get() = String.format("%.1f%% / hora", drainRatePerHour)
}
