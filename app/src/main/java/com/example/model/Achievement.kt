package com.example.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val requiredValue: Double,
    val unit: String,
    val category: AchievementCategory
)

enum class AchievementCategory {
    DISTANCE, ELEVATION, ACTIVITIES
}

object AchievementManager {
    val allAchievements = listOf(
        Achievement(
            id = "first_step",
            title = "Primeros Pasos",
            description = "Registra tu primera actividad GPS",
            iconName = "🌱",
            requiredValue = 1.0,
            unit = "actividad",
            category = AchievementCategory.ACTIVITIES
        ),
        Achievement(
            id = "dist_10",
            title = "Explorador Novato",
            description = "Acumula 10 km de distancia total",
            iconName = "🧭",
            requiredValue = 10.0,
            unit = "km",
            category = AchievementCategory.DISTANCE
        ),
        Achievement(
            id = "dist_50",
            title = "Ciclista Constante",
            description = "Acumula 50 km de distancia total",
            iconName = "🚴",
            requiredValue = 50.0,
            unit = "km",
            category = AchievementCategory.DISTANCE
        ),
        Achievement(
            id = "dist_200",
            title = "Gran Fondo",
            description = "Supera los 200 km recorridos",
            iconName = "🏆",
            requiredValue = 200.0,
            unit = "km",
            category = AchievementCategory.DISTANCE
        ),
        Achievement(
            id = "ele_500",
            title = "Escalador de Colinas",
            description = "Acumula 500 metros de desnivel positivo",
            iconName = "⛰️",
            requiredValue = 500.0,
            unit = "m",
            category = AchievementCategory.ELEVATION
        ),
        Achievement(
            id = "ele_2000",
            title = "Rey de la Montaña",
            description = "Acumula 2,000 metros de desnivel positivo",
            iconName = "👑",
            requiredValue = 2000.0,
            unit = "m",
            category = AchievementCategory.ELEVATION
        )
    )
}
