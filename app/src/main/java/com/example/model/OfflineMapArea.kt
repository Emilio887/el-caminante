package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OfflineStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    ERROR
}

@Entity(tableName = "offline_areas")
data class OfflineMapArea(
    @PrimaryKey val id: String,
    val name: String,
    val providerId: String,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val minZoom: Int = 11,
    val maxZoom: Int = 14,
    val totalTiles: Int = 0,
    val downloadedTiles: Int = 0,
    val sizeBytes: Long = 0L,
    val status: OfflineStatus = OfflineStatus.PENDING,
    val createdTimestamp: Long = System.currentTimeMillis()
) {
    val progressPercent: Float
        get() = if (totalTiles > 0) (downloadedTiles.toFloat() / totalTiles.toFloat()) * 100f else 0f

    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%.0f KB", sizeBytes / 1024.0)
        }
}
