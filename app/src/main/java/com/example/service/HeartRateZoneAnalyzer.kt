package com.example.service

import com.example.model.GpsPoint

data class HeartRateZoneStats(
    val zone1Minutes: Int, // Recovery (< 114 bpm)
    val zone2Minutes: Int, // Aerobic (114 - 132 bpm)
    val zone3Minutes: Int, // Tempo (133 - 151 bpm)
    val zone4Minutes: Int, // Threshold (152 - 170 bpm)
    val zone5Minutes: Int, // Anaerobic (> 170 bpm)
    val averageHeartRate: Int,
    val maxHeartRate: Int,
    val recommendedRecoveryHours: Int,
    val intensityAssessment: String
)

object HeartRateZoneAnalyzer {
    fun analyze(points: List<GpsPoint>): HeartRateZoneStats {
        val hrs = points.mapNotNull { it.heartRate }
        if (hrs.isEmpty()) {
            return HeartRateZoneStats(0, 0, 0, 0, 0, 0, 0, 12, "Moderada (Estimación general)")
        }

        var z1 = 0
        var z2 = 0
        var z3 = 0
        var z4 = 0
        var z5 = 0

        for (hr in hrs) {
            when {
                hr < 114 -> z1++
                hr in 114..132 -> z2++
                hr in 133..151 -> z3++
                hr in 152..170 -> z4++
                else -> z5++
            }
        }

        val avg = hrs.average().toInt()
        val max = hrs.maxOrNull() ?: 120

        val highIntensityMinutes = z4 + z5
        val recoveryHours = when {
            highIntensityMinutes > 45 -> 48
            highIntensityMinutes > 20 -> 36
            highIntensityMinutes > 5 -> 24
            else -> 12
        }

        val assessment = when {
            z5 > 10 -> "Muy Alta (Esfuerzo Anaeróbico / VO2 Max)"
            z4 > 15 -> "Alta (Umbral de Lactato)"
            z3 > 20 -> "Moderada-Alta (Ritmo Tempo Sostenido)"
            z2 > 30 -> "Aeróbica (Quema de grasas / Resistencia)"
            else -> "Recuperación / Paseo Suave"
        }

        return HeartRateZoneStats(
            zone1Minutes = z1,
            zone2Minutes = z2,
            zone3Minutes = z3,
            zone4Minutes = z4,
            zone5Minutes = z5,
            averageHeartRate = avg,
            maxHeartRate = max,
            recommendedRecoveryHours = recoveryHours,
            intensityAssessment = assessment
        )
    }
}
