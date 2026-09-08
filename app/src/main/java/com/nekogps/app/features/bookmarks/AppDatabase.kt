package com.nekogps.app.features.bookmarks

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

import com.nekogps.app.features.findmycar.ParkingDao
import com.nekogps.app.features.waypoints.WaypointDao

class Converters {
    @TypeConverter
    fun fromCategory(value: BookmarkCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): BookmarkCategory = BookmarkCategory.valueOf(value)
}

@Database(entities = [BookmarkEntity::class, ParkingLocation::class, WaypointEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun parkingDao(): ParkingDao
    abstract fun waypointDao(): WaypointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "neko_gps_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
