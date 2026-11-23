package com.example.format

import com.example.model.GpsPoint

object PrivacyTrimmer {
    /**
     * Trims roughly [trimDistanceMeters] (default 150m) from the beginning and end of a route
     * to protect user's home / parking privacy when publishing or sharing community routes.
     */
    fun trimTrack(points: List<GpsPoint>, trimDistanceMeters: Double = 150.0): List<GpsPoint> {
        if (points.size < 4) return points

        // Calculate cumulative distances from start
        var accumulatedStart = 0.0
        var startIndex = 0
        for (i in 0 until points.size - 1) {
            accumulatedStart += points[i].distanceTo(points[i + 1])
            if (accumulatedStart >= trimDistanceMeters) {
                startIndex = i + 1
                break
            }
        }

        // Calculate cumulative distances from end
        var accumulatedEnd = 0.0
        var endIndex = points.size - 1
        for (i in points.size - 1 downTo 1) {
            accumulatedEnd += points[i].distanceTo(points[i - 1])
            if (accumulatedEnd >= trimDistanceMeters) {
                endIndex = i - 1
                break
            }
        }

        return if (startIndex < endIndex && (endIndex - startIndex) >= 2) {
            points.subList(startIndex, endIndex + 1)
        } else {
            // If the route is too short to trim 150m both ways, return inner half
            val mid = points.size / 2
            points.subList(points.size / 4, (points.size * 3) / 4)
        }
    }
}
