package com.nekogps.app.features.bookmarks

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

import com.nekogps.app.features.findmycar.ParkingDao
import com.nekogps.app.features.stats.OdometerDao
import com.nekogps.app.features.stats.OdometerEntity
import com.nekogps.app.features.waypoints.WaypointDao
import com.nekogps.app.features.gamification.AchievementDao
import com.nekogps.app.features.gamification.AchievementEntity
import com.nekogps.app.features.gamification.LeaderboardDao
import com.nekogps.app.features.gamification.LeaderboardEntryEntity
import com.nekogps.app.features.gamification.LocationChallengeDao
import com.nekogps.app.features.gamification.LocationChallengeEntity
import com.nekogps.app.features.gamification.PhotoWaypointDao
import com.nekogps.app.features.gamification.PhotoWaypointEntity

class Converters {
    @TypeConverter
    fun fromCategory(value: BookmarkCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): BookmarkCategory = BookmarkCategory.valueOf(value)
}

@Database(entities = [
    BookmarkEntity::class,
    ParkingLocation::class,
    WaypointEntity::class,
    OdometerEntity::class,
    AchievementEntity::class,
    LeaderboardEntryEntity::class,
    LocationChallengeEntity::class,
    PhotoWaypointEntity::class
], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun parkingDao(): ParkingDao
    abstract fun waypointDao(): WaypointDao
    abstract fun odometerDao(): OdometerDao
    abstract fun achievementDao(): AchievementDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun locationChallengeDao(): LocationChallengeDao
    abstract fun photoWaypointDao(): PhotoWaypointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "neko_gps_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
