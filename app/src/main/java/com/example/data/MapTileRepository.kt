package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.example.map.MapTileEngine
import com.example.model.MapProvider
import com.example.model.MapTileEntity
import com.example.model.OfflineMapArea
import com.example.model.OfflineStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class MapTileRepository(
    private val mapTileDao: MapTileDao,
    private val offlineAreaDao: OfflineAreaDao,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    // 15MB in-memory bitmap cache for smooth map panning and zero disk stutter
    private val memoryCache = object : LruCache<String, Bitmap>(150) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return 1
        }
    }

    val offlineAreas: Flow<List<OfflineMapArea>> = offlineAreaDao.getAllAreas()
    val totalCachedTilesCount: Flow<Int> = mapTileDao.getTotalTileCount()
    val totalCachedBytes: Flow<Long?> = mapTileDao.getTotalSizeBytes()

    fun getTileKey(providerId: String, zoom: Int, x: Int, y: Int): String {
        return "${providerId}_${zoom}_${x}_${y}"
    }

    /**
     * Retrieves a map tile by first checking memory cache, then checking Room DB.
     * If absent from Room and network is reachable, it downloads the tile, caches it in Room,
     * and returns the decoded Bitmap.
     */
    suspend fun getTile(
        provider: MapProvider,
        zoom: Int,
        x: Int,
        y: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        val key = getTileKey(provider.id, zoom, x, y)

        // 1. In-memory cache
        memoryCache.get(key)?.let { return@withContext it }

        // 2. Room Database storage
        val entity = mapTileDao.getTileByKey(key)
        if (entity != null && entity.tileData.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeByteArray(entity.tileData, 0, entity.tileData.size)
            if (bitmap != null) {
                memoryCache.put(key, bitmap)
                return@withContext bitmap
            }
        }

        // 3. Online fetch & save to Room
        val url = provider.buildTileUrl(zoom, x, y)
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SenderoMTB-Android/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    // Store in Room
                    val newEntity = MapTileEntity(
                        key = key,
                        providerId = provider.id,
                        zoom = zoom,
                        x = x,
                        y = y,
                        tileData = bytes,
                        sizeBytes = bytes.size.toLong(),
                        cachedTimestamp = System.currentTimeMillis()
                    )
                    mapTileDao.insertTile(newEntity)

                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        memoryCache.put(key, bitmap)
                        return@withContext bitmap
                    }
                }
            }
        } catch (_: Exception) {
            // Offline and tile not found in Room
        }

        return@withContext null
    }

    /**
     * Downloads and caches all tiles within a specified geographic area into Room.
     */
    suspend fun cacheArea(
        area: OfflineMapArea,
        onProgress: (downloaded: Int, total: Int, bytes: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val provider = MapProvider.fromId(area.providerId)
        offlineAreaDao.insertArea(area.copy(status = OfflineStatus.DOWNLOADING))

        val tilesCoords = mutableListOf<Triple<Int, Int, Int>>() // zoom, x, y

        for (z in area.minZoom..area.maxZoom) {
            val minX = MapTileEngine.latLonToTileX(area.minLon, z)
            val maxX = MapTileEngine.latLonToTileX(area.maxLon, z)
            val minY = MapTileEngine.latLonToTileY(area.maxLat, z)
            val maxY = MapTileEngine.latLonToTileY(area.minLat, z)

            val startX = minOf(minX, maxX)
            val endX = maxOf(minX, maxX)
            val startY = minOf(minY, maxY)
            val endY = maxOf(minY, maxY)

            for (x in startX..endX) {
                for (y in startY..endY) {
                    tilesCoords.add(Triple(z, x, y))
                }
            }
        }

        val total = tilesCoords.size
        var downloaded = 0
        var totalBytes = 0L
        val batch = mutableListOf<MapTileEntity>()

        for (coord in tilesCoords) {
            val z = coord.first
            val x = coord.second
            val y = coord.third
            val key = getTileKey(provider.id, z, x, y)

            // Check if already in Room
            val existing = mapTileDao.getTileByKey(key)
            if (existing != null) {
                downloaded++
                totalBytes += existing.sizeBytes
            } else {
                val url = provider.buildTileUrl(z, x, y)
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "SenderoMTB-Android/1.0")
                        .build()
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            val entity = MapTileEntity(
                                key = key,
                                providerId = provider.id,
                                zoom = z,
                                x = x,
                                y = y,
                                tileData = bytes,
                                sizeBytes = bytes.size.toLong(),
                                cachedTimestamp = System.currentTimeMillis(),
                                areaId = area.id
                            )
                            batch.add(entity)
                            downloaded++
                            totalBytes += bytes.size.toLong()
                        }
                    }
                } catch (_: Exception) {}
            }

            // Batch insert to Room every 10 tiles or at end
            if (batch.size >= 10 || downloaded == total) {
                if (batch.isNotEmpty()) {
                    mapTileDao.insertTiles(batch)
                    batch.clear()
                }
                onProgress(downloaded, total, totalBytes)
            }
        }

        // Final commit to Room
        if (batch.isNotEmpty()) {
            mapTileDao.insertTiles(batch)
            batch.clear()
        }

        offlineAreaDao.updateArea(
            area.copy(
                status = OfflineStatus.COMPLETED,
                totalTiles = total,
                downloadedTiles = downloaded,
                sizeBytes = totalBytes
            )
        )
    }

    /**
     * Deletes an offline area and its stored tiles from Room.
     */
    suspend fun deleteArea(area: OfflineMapArea) = withContext(Dispatchers.IO) {
        mapTileDao.deleteTilesForArea(area.id)
        offlineAreaDao.deleteArea(area)
    }

    /**
     * Clears all cached tiles from Room.
     */
    suspend fun clearAllTiles() = withContext(Dispatchers.IO) {
        memoryCache.evictAll()
        mapTileDao.clearAllTiles()
    }
}
