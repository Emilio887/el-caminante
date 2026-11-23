package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.ui.graphics.vector.ImageVector

enum class ActivityType(
    val labelEs: String,
    val description: String,
    val icon: ImageVector,
    val calorieFactorPerKm: Double
) {
    MTB(
        labelEs = "MTB (Montaña)",
        description = "Ciclismo de montaña en pistas, senderos y trialeras",
        icon = Icons.Default.DirectionsBike,
        calorieFactorPerKm = 38.0
    ),
    ROAD_CYCLING(
        labelEs = "Ciclismo Carretera",
        description = "Ciclismo en asfalto, fondo y puertos",
        icon = Icons.Default.PedalBike,
        calorieFactorPerKm = 28.0
    ),
    HIKING(
        labelEs = "Senderismo",
        description = "Rutas a pie en naturaleza y montaña",
        icon = Icons.Default.Landscape,
        calorieFactorPerKm = 65.0
    ),
    RUNNING(
        labelEs = "Carrera (Trail/Asfalto)",
        description = "Running y trail running con ritmo por km",
        icon = Icons.Default.DirectionsRun,
        calorieFactorPerKm = 72.0
    ),
    WALKING(
        labelEs = "Caminata",
        description = "Paseos activos y marcha nórdica",
        icon = Icons.Default.DirectionsWalk,
        calorieFactorPerKm = 50.0
    );

    companion object {
        fun fromString(value: String): ActivityType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MTB
        }
    }
}
