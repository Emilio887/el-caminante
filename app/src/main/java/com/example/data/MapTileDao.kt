package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.MapTileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MapTileDao {

    @Query("SELECT * FROM map_tiles WHERE `key` = :key LIMIT 1")
    suspend fun getTileByKey(key: String): MapTileEntity?

    @Query("SELECT * FROM map_tiles WHERE providerId = :providerId AND zoom = :zoom AND x = :x AND y = :y LIMIT 1")
    suspend fun getTile(providerId: String, zoom: Int, x: Int, y: Int): MapTileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTile(tile: MapTileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTiles(tiles: List<MapTileEntity>)

    @Query("DELETE FROM map_tiles WHERE areaId = :areaId")
    suspend fun deleteTilesForArea(areaId: String)

    @Query("DELETE FROM map_tiles WHERE providerId = :providerId")
    suspend fun deleteTilesForProvider(providerId: String)

    @Query("DELETE FROM map_tiles")
    suspend fun clearAllTiles()

    @Query("SELECT COUNT(*) FROM map_tiles")
    fun getTotalTileCount(): Flow<Int>

    @Query("SELECT SUM(sizeBytes) FROM map_tiles")
    fun getTotalSizeBytes(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM map_tiles WHERE areaId = :areaId")
    suspend fun getTileCountForArea(areaId: String): Int

    @Query("SELECT SUM(sizeBytes) FROM map_tiles WHERE areaId = :areaId")
    suspend fun getSizeBytesForArea(areaId: String): Long?
}
