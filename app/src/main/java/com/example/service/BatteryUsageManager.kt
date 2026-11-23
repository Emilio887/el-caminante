package com.example.service

import com.example.model.BatteryLogSnapshot
import com.example.model.BatteryOptimizationTip
import com.example.model.BatteryUsageSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max

object BatteryUsageManager {

    private val _summary = MutableStateFlow(BatteryUsageSummary())
    val summary: StateFlow<BatteryUsageSummary> = _summary.asStateFlow()

    private var startTimestamp: Long = 0L
    private var initialBatteryLevel: Int = -1

    fun startSession(initialLevel: Int) {
        startTimestamp = System.currentTimeMillis()
        initialBatteryLevel = initialLevel
        _summary.value = BatteryUsageSummary(
            startLevelPercent = initialLevel,
            currentLevelPercent = initialLevel,
            totalDropPercent = 0,
            elapsedDurationMinutes = 0,
            drainRatePerHour = 6.5f, // Baseline initial estimate for outdoor GPS app
            estimatedRemainingHours = if (initialLevel > 0) initialLevel / 6.5f else 10f,
            snapshots = listOf(
                BatteryLogSnapshot(
                    timestamp = startTimestamp,
                    batteryLevelPercent = initialLevel,
                    temperatureCelsius = 25f,
                    voltageMilliVolts = 4100,
                    isCharging = false
                )
            ),
            optimizationTips = generateTips(initialLevel, 6.5f, 25f, false, true)
        )
    }

    fun logSnapshot(
        level: Int,
        temperatureCelsius: Float,
        voltageMilliVolts: Int,
        isCharging: Boolean,
        screenOn: Boolean
    ) {
        val now = System.currentTimeMillis()
        if (startTimestamp == 0L) {
            startSession(level)
            return
        }

        val elapsedMinutes = max(1, ((now - startTimestamp) / 60000L).toInt())
        val startLevel = if (initialBatteryLevel > 0) initialBatteryLevel else level
        val totalDrop = max(0, startLevel - level)

        // Calculate dynamic drain rate (%/hour)
        val calculatedDrainRate = if (elapsedMinutes >= 5 && totalDrop > 0) {
            (totalDrop.toFloat() / (elapsedMinutes / 60.0f)).coerceIn(2.0f, 40.0f)
        } else {
            // Adaptive estimation: baseline 8% with screen on, 4.5% with screen off/eco
            if (_summary.value.isEcoModeEnabled) 4.5f else if (screenOn) 8.2f else 5.0f
        }

        val remainingHours = if (calculatedDrainRate > 0.5f) {
            (level.toFloat() / calculatedDrainRate).coerceIn(0.1f, 48.0f)
        } else 24.0f

        val snapshot = BatteryLogSnapshot(
            timestamp = now,
            batteryLevelPercent = level,
            temperatureCelsius = temperatureCelsius,
            voltageMilliVolts = voltageMilliVolts,
            isCharging = isCharging,
            screenStateOn = screenOn,
            activityDurationMinutes = elapsedMinutes
        )

        _summary.update { curr ->
            val updatedSnapshots = (curr.snapshots + snapshot).takeLast(60)
            val maxTemp = max(curr.maxTemperatureCelsius, temperatureCelsius)
            val avgTemp = (curr.averageTemperatureCelsius * 0.8f + temperatureCelsius * 0.2f)

            curr.copy(
                currentLevelPercent = level,
                totalDropPercent = totalDrop,
                elapsedDurationMinutes = elapsedMinutes,
                drainRatePerHour = calculatedDrainRate,
                estimatedRemainingHours = remainingHours,
                averageTemperatureCelsius = avgTemp,
                maxTemperatureCelsius = maxTemp,
                snapshots = updatedSnapshots,
                optimizationTips = generateTips(level, calculatedDrainRate, maxTemp, curr.isEcoModeEnabled, screenOn)
            )
        }
    }

    fun toggleEcoMode() {
        _summary.update { curr ->
            val nextState = !curr.isEcoModeEnabled
            val newRate = if (nextState) curr.drainRatePerHour * 0.65f else curr.drainRatePerHour * 1.5f
            val remaining = if (newRate > 0) curr.currentLevelPercent / newRate else curr.estimatedRemainingHours

            curr.copy(
                isEcoModeEnabled = nextState,
                drainRatePerHour = newRate,
                estimatedRemainingHours = remaining,
                optimizationTips = generateTips(
                    curr.currentLevelPercent,
                    newRate,
                    curr.maxTemperatureCelsius,
                    nextState,
                    !nextState
                )
            )
        }
    }

    fun endSession() {
        startTimestamp = 0L
        initialBatteryLevel = -1
    }

    fun generateTips(
        batteryLevel: Int,
        drainRate: Float,
        maxTemp: Float,
        isEcoEnabled: Boolean,
        screenOn: Boolean
    ): List<BatteryOptimizationTip> {
        val tips = mutableListOf<BatteryOptimizationTip>()

        if (!isEcoEnabled) {
            tips.add(
                BatteryOptimizationTip(
                    id = "tip_eco_gps",
                    title = "Activar Modo Eco GPS",
                    description = "Reduce la tasa de muestreo GPS en tramos rectos de 1s a 3s para prolongar la autonomía hasta un 35%.",
                    estimatedSavingsPercent = 35,
                    actionType = "ECO_GPS"
                )
            )
        }

        if (screenOn) {
            tips.add(
                BatteryOptimizationTip(
                    id = "tip_auto_screen",
                    title = "Configurar Auto-Apagado de Pantalla",
                    description = "La pantalla es el componente de mayor consumo. Mantén la pantalla apagada y usa avisos sonoros de desvío.",
                    estimatedSavingsPercent = 45,
                    actionType = "AUTO_SCREEN_OFF"
                )
            )
        }

        if (maxTemp > 38f) {
            tips.add(
                BatteryOptimizationTip(
                    id = "tip_temperature",
                    title = "Temperatura Elevada (${String.format("%.1f", maxTemp)}°C)",
                    description = "El calentamiento acelera la degradación de la batería. Evita colocar el soporte del manillar bajo el sol abrasador constante.",
                    estimatedSavingsPercent = 15,
                    actionType = "HIGH_TEMP"
                )
            )
        }

        if (batteryLevel <= 25) {
            tips.add(
                BatteryOptimizationTip(
                    id = "tip_critical_battery",
                    title = "Batería en Reserva Crítica ($batteryLevel%)",
                    description = "Desactiva los sensores Bluetooth no indispensables y mantén el mapa en caché sin descargas activas.",
                    estimatedSavingsPercent = 20,
                    actionType = "CRITICAL"
                )
            )
        }

        return tips
    }
}
