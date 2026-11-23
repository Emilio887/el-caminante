package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.ActivityRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY startTime DESC")
    fun getAllActivities(): Flow<List<ActivityRecord>>

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    suspend fun getActivityById(id: String): ActivityRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityRecord)

    @Update
    suspend fun updateActivity(activity: ActivityRecord)

    @Delete
    suspend fun deleteActivity(activity: ActivityRecord)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: String)

    @Query("SELECT SUM(distanceMeters) FROM activities")
    fun getTotalDistanceMeters(): Flow<Double?>

    @Query("SELECT SUM(elevationGain) FROM activities")
    fun getTotalElevationGain(): Flow<Double?>

    @Query("SELECT SUM(totalDurationMillis) FROM activities")
    fun getTotalDurationMillis(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM activities")
    fun getActivityCount(): Flow<Int>
}
