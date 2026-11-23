package com.example.data

import com.example.model.ActivityType
import com.example.model.GpsPoint
import com.example.model.Route
import kotlin.math.cos
import kotlin.math.sin

object SampleRoutes {

    fun generateRealisticTrack(
        startLat: Double,
        startLon: Double,
        startAlt: Double,
        totalPoints: Int,
        latDelta: Double,
        lonDelta: Double,
        climbPeak: Double
    ): List<GpsPoint> {
        val points = mutableListOf<GpsPoint>()
        var currentLat = startLat
        var currentLon = startLon
        var currentAlt = startAlt
        val baseTime = System.currentTimeMillis() - 7200000L

        for (i in 0 until totalPoints) {
            val progress = i.toDouble() / totalPoints.toDouble()
            // Add slight curve
            val curve = sin(progress * Math.PI * 2.5) * 0.003
            val lat = startLat + (latDelta * progress) + curve
            val lon = startLon + (lonDelta * progress) + cos(progress * Math.PI * 2.0) * 0.002
            // Elevation profile shaped with a peak
            val alt = startAlt + sin(progress * Math.PI) * climbPeak + (sin(i * 0.4) * 8.0)
            val speed = 3.8f + (sin(i * 0.2).toFloat() * 1.5f).coerceAtLeast(0.5f)

            points.add(
                GpsPoint(
                    latitude = lat,
                    longitude = lon,
                    altitude = alt,
                    speed = speed,
                    timestamp = baseTime + (i * 12000L),
                    accuracy = 4.2f,
                    bearing = (progress * 360f).toFloat() % 360f,
                    heartRate = (135 + sin(progress * Math.PI) * 35).toInt(),
                    cadence = (78 + cos(progress * Math.PI) * 12).toInt(),
                    power = (190 + sin(progress * Math.PI) * 90).toInt()
                )
            )
        }
        return points
    }

    fun getInitialRoutes(): List<Route> {
        val guadarramaPoints = generateRealisticTrack(
            startLat = 40.7512,
            startLon = -4.0125,
            startAlt = 1180.0,
            totalPoints = 55,
            latDelta = 0.045,
            lonDelta = 0.038,
            climbPeak = 620.0
        )

        val cercedillaPoints = generateRealisticTrack(
            startLat = 40.7380,
            startLon = -4.0550,
            startAlt = 1210.0,
            totalPoints = 42,
            latDelta = 0.032,
            lonDelta = 0.025,
            climbPeak = 480.0
        )

        val picosPoints = generateRealisticTrack(
            startLat = 43.1850,
            startLon = -4.8210,
            startAlt = 950.0,
            totalPoints = 60,
            latDelta = 0.052,
            lonDelta = -0.040,
            climbPeak = 890.0
        )

        val collserolaPoints = generateRealisticTrack(
            startLat = 41.4280,
            startLon = 2.1120,
            startAlt = 280.0,
            totalPoints = 38,
            latDelta = 0.028,
            lonDelta = 0.022,
            climbPeak = 310.0
        )

        val sierraNevadaPoints = generateRealisticTrack(
            startLat = 37.0950,
            startLon = -3.3950,
            startAlt = 2100.0,
            totalPoints = 65,
            latDelta = 0.060,
            lonDelta = -0.045,
            climbPeak = 950.0
        )

        return listOf(
            Route(
                id = "route_guadarrama_fuenfria",
                name = "MTB Alto del León y Fuenfría",
                description = "Espectacular ruta circular de puro MTB entre pinares centenarios, subida por pista ancha y bajada técnica por sendero GR-10.",
                activityType = ActivityType.MTB,
                points = guadarramaPoints,
                distanceMeters = 38400.0,
                elevationGain = 760.0,
                elevationLoss = 760.0,
                difficulty = "Difícil",
                authorNickname = "CarlosRider92",
                province = "Madrid",
                isFavorite = true,
                isCommunity = true,
                privacyTrimmed = true
            ),
            Route(
                id = "route_cercedilla_puricelli",
                name = "Pista Puricelli y Calzada Romana",
                description = "Ruta clásica y fluida de MTB y senderismo por el Valle de la Fuenfría con miradores panorámicos.",
                activityType = ActivityType.MTB,
                points = cercedillaPoints,
                distanceMeters = 24100.0,
                elevationGain = 510.0,
                elevationLoss = 510.0,
                difficulty = "Moderada",
                authorNickname = "SaraMTB_Trail",
                province = "Madrid",
                isFavorite = false,
                isCommunity = true,
                privacyTrimmed = true
            ),
            Route(
                id = "route_picos_europa",
                name = "Picos de Europa: Anillo Central",
                description = "Aventura alpina con desniveles pronunciados, vistas a Peña Vieja y pasos rocosos inolvidables.",
                activityType = ActivityType.HIKING,
                points = picosPoints,
                distanceMeters = 19800.0,
                elevationGain = 980.0,
                elevationLoss = 980.0,
                difficulty = "Muy Difícil",
                authorNickname = "GuiaCantabria",
                province = "Asturias / Cantabria",
                isFavorite = true,
                isCommunity = true,
                privacyTrimmed = true
            ),
            Route(
                id = "route_collserola_gravel",
                name = "Collserola: Carretera de les Aigües y Tibidabo",
                description = "Ruta rápida mixta para gravel y carretera con vistas a toda Barcelona y el mar Mediterráneo.",
                activityType = ActivityType.ROAD_CYCLING,
                points = collserolaPoints,
                distanceMeters = 31200.0,
                elevationGain = 450.0,
                elevationLoss = 450.0,
                difficulty = "Moderada",
                authorNickname = "BarnaCyclist",
                province = "Barcelona",
                isFavorite = false,
                isCommunity = true,
                privacyTrimmed = true
            ),
            Route(
                id = "route_sierra_nevada",
                name = "Descenso Integral Veleta - Capileira",
                description = "El gran clásico de alta montaña andaluz, descendiendo desde las altas cumbres hacia la Alpujarra.",
                activityType = ActivityType.MTB,
                points = sierraNevadaPoints,
                distanceMeters = 44500.0,
                elevationGain = 1120.0,
                elevationLoss = 2100.0,
                difficulty = "Muy Difícil",
                authorNickname = "AlpujarraBiker",
                province = "Granada",
                isFavorite = true,
                isCommunity = true,
                privacyTrimmed = true
            )
        )
    }

    fun getInitialActivities(): List<com.example.model.ActivityRecord> {
        val now = System.currentTimeMillis()
        val oneDay = 86400000L

        return listOf(
            com.example.model.ActivityRecord(
                id = "act_hist_1",
                title = "Salida Fuenfría MTB",
                activityType = ActivityType.MTB,
                startTime = now - (oneDay * 2),
                endTime = now - (oneDay * 2) + 7200000L,
                totalDurationMillis = 7200000L,
                movingDurationMillis = 6800000L,
                distanceMeters = 34500.0,
                avgSpeedKmh = 17.5f,
                maxSpeedKmh = 42.1f,
                elevationGain = 720.0,
                elevationLoss = 720.0,
                minAltitude = 1180.0,
                maxAltitude = 1790.0,
                avgHeartRate = 148,
                maxHeartRate = 176,
                avgCadence = 82,
                avgPower = 215,
                notes = "Buenas sensaciones, terreno seco y rápido."
            ),
            com.example.model.ActivityRecord(
                id = "act_hist_2",
                title = "Entrenamiento Carretera Tibidabo",
                activityType = ActivityType.ROAD_CYCLING,
                startTime = now - (oneDay * 4),
                endTime = now - (oneDay * 4) + 5400000L,
                totalDurationMillis = 5400000L,
                movingDurationMillis = 5200000L,
                distanceMeters = 42100.0,
                avgSpeedKmh = 26.2f,
                maxSpeedKmh = 54.0f,
                elevationGain = 580.0,
                elevationLoss = 580.0,
                minAltitude = 80.0,
                maxAltitude = 512.0,
                avgHeartRate = 155,
                maxHeartRate = 182,
                avgCadence = 88,
                avgPower = 240,
                notes = "Series en subida al mirador."
            ),
            com.example.model.ActivityRecord(
                id = "act_hist_3",
                title = "Circular Cercedilla Singletrack",
                activityType = ActivityType.MTB,
                startTime = now - (oneDay * 7),
                endTime = now - (oneDay * 7) + 6100000L,
                totalDurationMillis = 6100000L,
                movingDurationMillis = 5700000L,
                distanceMeters = 26800.0,
                avgSpeedKmh = 15.2f,
                maxSpeedKmh = 38.4f,
                elevationGain = 640.0,
                elevationLoss = 640.0,
                minAltitude = 1200.0,
                maxAltitude = 1750.0,
                avgHeartRate = 142,
                maxHeartRate = 169,
                notes = "Trialeras técnicas y bajada por bosque."
            ),
            com.example.model.ActivityRecord(
                id = "act_hist_4",
                title = "Travesía Picos de Europa",
                activityType = ActivityType.HIKING,
                startTime = now - (oneDay * 12),
                endTime = now - (oneDay * 12) + 14400000L,
                totalDurationMillis = 14400000L,
                movingDurationMillis = 13200000L,
                distanceMeters = 19400.0,
                avgSpeedKmh = 4.8f,
                maxSpeedKmh = 7.5f,
                elevationGain = 980.0,
                elevationLoss = 980.0,
                minAltitude = 950.0,
                maxAltitude = 1930.0,
                avgHeartRate = 128,
                notes = "Paso de montaña y vistas panorámicas espectaculares."
            ),
            com.example.model.ActivityRecord(
                id = "act_hist_5",
                title = "Gran Fondo Sierra Nevada",
                activityType = ActivityType.MTB,
                startTime = now - (oneDay * 20),
                endTime = now - (oneDay * 20) + 12600000L,
                totalDurationMillis = 12600000L,
                movingDurationMillis = 11800000L,
                distanceMeters = 54200.0,
                avgSpeedKmh = 16.5f,
                maxSpeedKmh = 48.0f,
                elevationGain = 1280.0,
                elevationLoss = 1280.0,
                minAltitude = 1600.0,
                maxAltitude = 2850.0,
                avgHeartRate = 158,
                avgCadence = 79,
                avgPower = 230,
                notes = "Salida épica de altura con mucho desnivel."
            ),
            com.example.model.ActivityRecord(
                id = "act_hist_6",
                title = "Rodaje Gravel y Pistas",
                activityType = ActivityType.ROAD_CYCLING,
                startTime = now - (oneDay * 35),
                endTime = now - (oneDay * 35) + 7200000L,
                totalDurationMillis = 7200000L,
                movingDurationMillis = 6900000L,
                distanceMeters = 48000.0,
                avgSpeedKmh = 23.5f,
                maxSpeedKmh = 44.0f,
                elevationGain = 490.0,
                elevationLoss = 490.0,
                minAltitude = 210.0,
                maxAltitude = 700.0,
                avgHeartRate = 138,
                notes = "Pistas de grava compacta con ritmo constante."
            ),
            com.example.model.ActivityRecord(
                id = "act_hist_7",
                title = "MTB Alto del León",
                activityType = ActivityType.MTB,
                startTime = now - (oneDay * 50),
                endTime = now - (oneDay * 50) + 7800000L,
                totalDurationMillis = 7800000L,
                movingDurationMillis = 7200000L,
                distanceMeters = 38000.0,
                avgSpeedKmh = 17.0f,
                maxSpeedKmh = 45.2f,
                elevationGain = 810.0,
                elevationLoss = 810.0,
                minAltitude = 1100.0,
                maxAltitude = 1780.0,
                avgHeartRate = 146,
                notes = "Subida técnica por pedregal y bajada rápida."
            )
        )
    }
}
