package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.model.ActivityRecord
import com.example.model.MapTileEntity
import com.example.model.OfflineMapArea
import com.example.model.Route

@Database(
    entities = [
        Route::class,
        ActivityRecord::class,
        OfflineMapArea::class,
        MapTileEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun activityDao(): ActivityDao
    abstract fun offlineAreaDao(): OfflineAreaDao
    abstract fun mapTileDao(): MapTileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sendero_mtb_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
