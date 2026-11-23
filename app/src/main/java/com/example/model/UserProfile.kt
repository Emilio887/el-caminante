package com.example.model

data class UserProfile(
    val name: String = "Explorador MTB",
    val email: String = "caminante@senderomtb.com",
    val age: Int = 32,
    val weightKg: Float = 72.0f,
    val heightCm: Int = 178,
    val preferredActivity: String = "Ciclismo de Montaña (MTB)",
    val emergencyContact: String = "+34 600 123 456",
    val autoPauseEnabled: Boolean = true,
    val voicePromptsEnabled: Boolean = true,
    val weeklyDistanceGoalKm: Float = 50.0f,
    val weeklyElevationGoalM: Float = 1000.0f,
    val isDarkModeEnabled: Boolean = false,
    val useDynamicColor: Boolean = true,
    val batterySaverBelow20Enabled: Boolean = true
)
