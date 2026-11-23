package com.example.map

import android.content.Context
import android.graphics.Bitmap
import com.example.data.AppDatabase
import com.example.data.MapTileRepository
import com.example.model.MapProvider
import com.example.model.OfflineMapArea
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

object MapTileEngine {

    @Volatile
    private var repositoryInstance: MapTileRepository? = null

    fun getRepository(context: Context): MapTileRepository {
        return repositoryInstance ?: synchronized(this) {
            val db = AppDatabase.getDatabase(context)
            val repo = MapTileRepository(db.mapTileDao(), db.offlineAreaDao())
            repositoryInstance = repo
            repo
        }
    }

    fun getTileKey(providerId: String, zoom: Int, x: Int, y: Int): String {
        return "${providerId}_${zoom}_${x}_${y}"
    }

    suspend fun getTileBitmap(
        context: Context,
        provider: MapProvider,
        zoom: Int,
        x: Int,
        y: Int
    ): Bitmap? {
        return getRepository(context).getTile(provider, zoom, x, y)
    }

    fun latLonToTileX(lon: Double, zoom: Int): Int {
        val n = 1 shl zoom
        return floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
    }

    fun latLonToTileY(lat: Double, zoom: Int): Int {
        val n = 1 shl zoom
        val latRad = Math.toRadians(lat)
        val y = (1.0 - ln(tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / Math.PI) / 2.0 * n
        return floor(y).toInt().coerceIn(0, n - 1)
    }

    suspend fun downloadAreaOffline(
        context: Context,
        area: OfflineMapArea,
        onProgress: (downloaded: Int, total: Int, bytes: Long) -> Unit
    ) {
        getRepository(context).cacheArea(area, onProgress)
    }
}
