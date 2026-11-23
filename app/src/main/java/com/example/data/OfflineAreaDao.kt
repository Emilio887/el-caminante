package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.OfflineMapArea
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineAreaDao {
    @Query("SELECT * FROM offline_areas ORDER BY createdTimestamp DESC")
    fun getAllAreas(): Flow<List<OfflineMapArea>>

    @Query("SELECT * FROM offline_areas WHERE id = :id LIMIT 1")
    suspend fun getAreaById(id: String): OfflineMapArea?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArea(area: OfflineMapArea)

    @Update
    suspend fun updateArea(area: OfflineMapArea)

    @Delete
    suspend fun deleteArea(area: OfflineMapArea)

    @Query("DELETE FROM offline_areas WHERE id = :id")
    suspend fun deleteAreaById(id: String)
}
