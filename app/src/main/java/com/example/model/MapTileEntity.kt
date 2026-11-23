package com.example.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "map_tiles",
    indices = [
        Index(value = ["providerId", "zoom", "x", "y"], unique = true),
        Index(value = ["areaId"])
    ]
)
data class MapTileEntity(
    @PrimaryKey
    val key: String, // e.g., "opentopo_14_8123_6045"
    val providerId: String,
    val zoom: Int,
    val x: Int,
    val y: Int,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val tileData: ByteArray,
    val sizeBytes: Long = tileData.size.toLong(),
    val cachedTimestamp: Long = System.currentTimeMillis(),
    val areaId: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MapTileEntity
        return key == other.key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}
