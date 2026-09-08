package com.nekogps.app.features.gamification

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AchievementEntity::class,
        LeaderboardEntryEntity::class,
        LocationChallengeEntity::class,
        PhotoWaypointEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GamificationDatabase : RoomDatabase() {
    abstract fun achievementDao(): AchievementDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun locationChallengeDao(): LocationChallengeDao
    abstract fun photoWaypointDao(): PhotoWaypointDao

    companion object {
        @Volatile
        private var INSTANCE: GamificationDatabase? = null

        fun getInstance(context: Context): GamificationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GamificationDatabase::class.java,
                    "gamification_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
