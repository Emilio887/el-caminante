package com.example

import com.example.data.MapTileDao
import com.example.data.MapTileRepository
import com.example.data.OfflineAreaDao
import com.example.format.GpxHandler
import com.example.format.PrivacyTrimmer
import com.example.gps.OffTrackCalculator
import com.example.map.MapTileEngine
import com.example.model.GpsPoint
import com.example.model.MapProvider
import com.example.model.MapTileEntity
import com.example.model.OfflineMapArea
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun privacyTrimmerRemoves150mFromEndpoints() {
        val points = mutableListOf<GpsPoint>()
        // 20 points spaced ~50 meters apart
        for (i in 0 until 20) {
            points.add(
                GpsPoint(
                    latitude = 40.0 + (i * 0.0005),
                    longitude = -4.0,
                    altitude = 1000.0 + i
                )
            )
        }

        val trimmed = PrivacyTrimmer.trimTrack(points, 150.0)
        assertTrue(trimmed.size < points.size)
        // Ensure starting point has changed (at least ~150m further down track)
        assertTrue(trimmed.first().latitude > points.first().latitude)
        assertTrue(trimmed.last().latitude < points.last().latitude)
    }

    @Test
    fun offTrackCalculatorIdentifiesDistanceToRoute() {
        val routePoints = listOf(
            GpsPoint(latitude = 40.0000, longitude = -4.0000),
            GpsPoint(latitude = 40.0010, longitude = -4.0000),
            GpsPoint(latitude = 40.0020, longitude = -4.0000)
        )

        // Point directly on segment
        val onTrackPoint = GpsPoint(latitude = 40.0005, longitude = -4.0000)
        val onTrackDist = OffTrackCalculator.getDistanceToRoute(onTrackPoint, routePoints)
        assertTrue(onTrackDist < 2.0)

        // Point 200m away
        val offTrackPoint = GpsPoint(latitude = 40.0005, longitude = -3.9975)
        val offTrackDist = OffTrackCalculator.getDistanceToRoute(offTrackPoint, routePoints)
        assertTrue(offTrackDist > 100.0)
    }

    @Test
    fun gpxExportProducesValidXml() {
        val points = listOf(
            GpsPoint(latitude = 40.7512, longitude = -4.0125, altitude = 1180.0, speed = 5.2f),
            GpsPoint(latitude = 40.7520, longitude = -4.0130, altitude = 1188.0, speed = 4.8f)
        )
        val xml = GpxHandler.exportToGpx("Ruta Guadarrama", "Prueba", points)
        assertTrue(xml.contains("<gpx version=\"1.1\""))
        assertTrue(xml.contains("lat=\"40.7512\""))
        assertTrue(xml.contains("<ele>1180.0</ele>"))
    }

    @Test
    fun mapTileCoordinateCalculationsAreAccurate() {
        // Madrid coordinates (~40.4168, -3.7038) at zoom 14
        val tileX = MapTileEngine.latLonToTileX(-3.7038, 14)
        val tileY = MapTileEngine.latLonToTileY(40.4168, 14)

        assertTrue(tileX in 8000..8100)
        assertTrue(tileY in 6100..6300)
    }

    @Test
    fun mapTileEntityKeyMatchesStructure() {
        val entity = MapTileEntity(
            key = "opentopo_14_8023_6214",
            providerId = "opentopo",
            zoom = 14,
            x = 8023,
            y = 6214,
            tileData = byteArrayOf(1, 2, 3, 4)
        )
        assertEquals("opentopo_14_8023_6214", entity.key)
        assertEquals(4L, entity.sizeBytes)
    }

    @Test
    fun mapTileRepositoryTileKeyGeneration() {
        val mockTileDao = object : MapTileDao {
            val store = mutableMapOf<String, MapTileEntity>()
            override suspend fun getTileByKey(key: String): MapTileEntity? = store[key]
            override suspend fun getTile(providerId: String, zoom: Int, x: Int, y: Int): MapTileEntity? = store["${providerId}_${zoom}_${x}_${y}"]
            override suspend fun insertTile(tile: MapTileEntity) { store[tile.key] = tile }
            override suspend fun insertTiles(tiles: List<MapTileEntity>) { tiles.forEach { store[it.key] = it } }
            override suspend fun deleteTilesForArea(areaId: String) { store.values.removeAll { it.areaId == areaId } }
            override suspend fun deleteTilesForProvider(providerId: String) { store.values.removeAll { it.providerId == providerId } }
            override suspend fun clearAllTiles() { store.clear() }
            override fun getTotalTileCount(): Flow<Int> = flowOf(store.size)
            override fun getTotalSizeBytes(): Flow<Long?> = flowOf(store.values.sumOf { it.sizeBytes })
            override suspend fun getTileCountForArea(areaId: String): Int = store.values.count { it.areaId == areaId }
            override suspend fun getSizeBytesForArea(areaId: String): Long? = store.values.filter { it.areaId == areaId }.sumOf { it.sizeBytes }
        }

        val mockAreaDao = object : OfflineAreaDao {
            val areas = mutableMapOf<String, OfflineMapArea>()
            override fun getAllAreas(): Flow<List<OfflineMapArea>> = flowOf(areas.values.toList())
            override suspend fun getAreaById(id: String): OfflineMapArea? = areas[id]
            override suspend fun insertArea(area: OfflineMapArea) { areas[area.id] = area }
            override suspend fun updateArea(area: OfflineMapArea) { areas[area.id] = area }
            override suspend fun deleteArea(area: OfflineMapArea) { areas.remove(area.id) }
            override suspend fun deleteAreaById(id: String) { areas.remove(id) }
        }

        val repository = MapTileRepository(mockTileDao, mockAreaDao)
        val key = repository.getTileKey("cyclosm", 12, 2048, 1530)
        assertEquals("cyclosm_12_2048_1530", key)

        runBlocking {
            val tile = MapTileEntity(
                key = key,
                providerId = "cyclosm",
                zoom = 12,
                x = 2048,
                y = 1530,
                tileData = byteArrayOf(10, 20, 30),
                areaId = "area_test"
            )
            mockTileDao.insertTile(tile)
            val retrieved = mockTileDao.getTileByKey(key)
            assertNotNull(retrieved)
            assertEquals(3L, retrieved?.sizeBytes)

            // Test area deletion clears matching tiles from Room
            repository.deleteArea(OfflineMapArea("area_test", "Test", "cyclosm", 0.0, 0.0, 0.0, 0.0))
            assertNull(mockTileDao.getTileByKey(key))
        }
    }

    @Test
    fun batteryUsageManagerCalculatesDrainRateAndTips() {
        val manager = com.example.service.BatteryUsageManager
        manager.startSession(90)
        assertEquals(90, manager.summary.value.currentLevelPercent)
        assertEquals(90, manager.summary.value.startLevelPercent)

        // Log snapshot after battery drop
        manager.logSnapshot(
            level = 88,
            temperatureCelsius = 28.5f,
            voltageMilliVolts = 4050,
            isCharging = false,
            screenOn = true
        )

        assertEquals(88, manager.summary.value.currentLevelPercent)
        assertEquals(2, manager.summary.value.totalDropPercent)
        assertTrue(manager.summary.value.estimatedRemainingHours > 0f)

        // Toggle Eco Mode
        val wasEco = manager.summary.value.isEcoModeEnabled
        manager.toggleEcoMode()
        assertEquals(!wasEco, manager.summary.value.isEcoModeEnabled)

        // Generate Tips test
        val tips = manager.generateTips(
            batteryLevel = 18,
            drainRate = 14.5f,
            maxTemp = 39.5f,
            isEcoEnabled = false,
            screenOn = true
        )
        assertTrue(tips.any { it.actionType == "CRITICAL" })
        assertTrue(tips.any { it.actionType == "HIGH_TEMP" })
        assertTrue(tips.any { it.actionType == "ECO_GPS" })
        assertTrue(tips.any { it.actionType == "AUTO_SCREEN_OFF" })
    }

    @Test
    fun chartDataPointModelVerification() {
        val pt = com.example.ui.components.ChartDataPoint(
            label = "Sab",
            distanceKm = 48.5f,
            elevationGainM = 920f,
            dateLabel = "Sábado 15"
        )
        assertEquals("Sab", pt.label)
        assertEquals(48.5f, pt.distanceKm)
        assertEquals(920f, pt.elevationGainM)
        assertEquals("Sábado 15", pt.dateLabel)
    }

    @Test
    fun bleSensorModelCalculations() {
        val sensor = com.example.model.BleSensor(
            address = "AA:BB:CC:DD:EE:01",
            name = "Garmin HRM-Dual",
            type = com.example.model.BleSensorType.HEART_RATE,
            connectionState = com.example.model.BleConnectionState.CONNECTED,
            isPaired = true,
            autoConnect = true,
            batteryPercent = 95,
            liveReading = 145,
            rssi = -55
        )
        assertTrue(sensor.isConnected)
        assertEquals("Excelente", sensor.signalQuality)
        assertEquals("ppm", sensor.type.unit)

        val weakSensor = sensor.copy(rssi = -82, connectionState = com.example.model.BleConnectionState.DISCONNECTED)
        assertEquals("Débil", weakSensor.signalQuality)
        org.junit.Assert.assertFalse(weakSensor.isConnected)
    }

    @Test
    fun bleSensorManagerPairingFlow() {
        val manager = com.example.sensor.BleSensorManager
        val initialPaired = manager.pairedSensors.value
        assertTrue(initialPaired.any { it.type == com.example.model.BleSensorType.HEART_RATE })
        assertTrue(initialPaired.any { it.type == com.example.model.BleSensorType.SPEED_CADENCE })

        val newSensor = com.example.model.BleSensor(
            address = "11:22:33:44:55:66",
            name = "Wahoo Blue SC",
            type = com.example.model.BleSensorType.SPEED_CADENCE,
            rssi = -62
        )

        // Pair new sensor
        manager.pairSensor(newSensor)
        assertTrue(manager.pairedSensors.value.any { it.address == "11:22:33:44:55:66" })

        // Toggle auto connect
        val autoBefore = manager.pairedSensors.value.first { it.address == "11:22:33:44:55:66" }.autoConnect
        manager.toggleAutoConnect("11:22:33:44:55:66")
        val autoAfter = manager.pairedSensors.value.first { it.address == "11:22:33:44:55:66" }.autoConnect
        assertEquals(!autoBefore, autoAfter)

        // Unpair sensor
        manager.unpairSensor("11:22:33:44:55:66")
        org.junit.Assert.assertFalse(manager.pairedSensors.value.any { it.address == "11:22:33:44:55:66" })
    }
}

