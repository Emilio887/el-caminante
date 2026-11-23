package com.example.gps

import com.example.model.GpsPoint
import kotlin.math.min

object OffTrackCalculator {

    /**
     * Finds the minimum distance in meters between [currentLocation] and the polyline defined by [routePoints].
     */
    fun getDistanceToRoute(currentLocation: GpsPoint, routePoints: List<GpsPoint>): Double {
        if (routePoints.isEmpty()) return 0.0
        if (routePoints.size == 1) return currentLocation.distanceTo(routePoints[0])

        var minDistance = Double.MAX_VALUE

        // Sample along segments
        for (i in 0 until routePoints.size - 1) {
            val p1 = routePoints[i]
            val p2 = routePoints[i + 1]
            val dist = pointToSegmentDistance(currentLocation, p1, p2)
            if (dist < minDistance) {
                minDistance = dist
            }
        }

        return minDistance
    }

    private fun pointToSegmentDistance(p: GpsPoint, a: GpsPoint, b: GpsPoint): Double {
        val distAB = a.distanceTo(b)
        if (distAB < 1.0) return p.distanceTo(a)

        // Equirectangular projection local approximation
        val midLat = Math.toRadians((a.latitude + b.latitude) / 2.0)
        val cosLat = Math.cos(midLat)
        val kx = 111320.0 * cosLat
        val ky = 110540.0

        val ax = a.longitude * kx
        val ay = a.latitude * ky
        val bx = b.longitude * kx
        val by = b.latitude * ky
        val px = p.longitude * kx
        val py = p.latitude * ky

        val abx = bx - ax
        val aby = by - ay
        val apx = px - ax
        val apy = py - ay

        val ab2 = abx * abx + aby * aby
        val t = if (ab2 > 0.0) ((apx * abx + apy * aby) / ab2).coerceIn(0.0, 1.0) else 0.0

        val projX = ax + t * abx
        val projY = ay + t * aby
        val dx = px - projX
        val dy = py - projY

        return Math.sqrt(dx * dx + dy * dy)
    }
}
