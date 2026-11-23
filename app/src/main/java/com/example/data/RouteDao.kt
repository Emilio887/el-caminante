package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.Route
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY createdTimestamp DESC")
    fun getAllRoutes(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE isCommunity = 0 ORDER BY createdTimestamp DESC")
    fun getMyRoutes(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE isCommunity = 1 ORDER BY createdTimestamp DESC")
    fun getCommunityRoutes(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE id = :id LIMIT 1")
    suspend fun getRouteById(id: String): Route?

    @Query("SELECT * FROM routes WHERE isFavorite = 1")
    fun getFavoriteRoutes(): Flow<List<Route>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: Route)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<Route>)

    @Update
    suspend fun updateRoute(route: Route)

    @Delete
    suspend fun deleteRoute(route: Route)

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteRouteById(id: String)
}
