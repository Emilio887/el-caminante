package com.example.model

data class GpsPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speed: Float = 0f, // in m/s
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy: Float = 0f,
    val bearing: Float = 0f,
    val heartRate: Int? = null,
    val cadence: Int? = null,
    val power: Int? = null
) {
    val speedKmh: Float
        get() = speed * 3.6f

    fun distanceTo(other: GpsPoint): Double {
        val lat1 = Math.toRadians(latitude)
        val lon1 = Math.toRadians(longitude)
        val lat2 = Math.toRadians(other.latitude)
        val lon2 = Math.toRadians(other.longitude)
        val dLat = lat2 - lat1
        val dLon = lon2 - lon1
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return 6371000.0 * c // in meters
    }
}
