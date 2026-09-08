package com.nekogps.app.features.gamification

import android.content.Context
import com.nekogps.app.features.bookmarks.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * LocationChallengeManager - creates and tracks location-based challenges.
 * Challenges like visit 5 different locations, drive 50km in a day, etc.
 */
class LocationChallengeManager(context: Context) {
    private val dao = AppDatabase.getInstance(context).locationChallengeDao()

    val allChallenges: Flow<List<LocationChallengeEntity>> = dao.getAllChallenges()
    val activeChallenges: Flow<List<LocationChallengeEntity>> = dao.getActiveChallenges()
    val completedChallenges: Flow<List<LocationChallengeEntity>> = dao.getCompletedChallenges()

    /**
     * Define available location challenges.
     */
    fun getDefaultChallenges(): List<LocationChallengeDefinition> {
        return listOf(
            LocationChallengeDefinition(
                key = "visit_5_locations",
                name = "World Traveler",
                description = "Visit 5 different locations",
                icon = "🌎",
                targetValue = 5.0
            ),
            LocationChallengeDefinition(
                key = "drive_50km_day",
                name = "Daily Commuter",
                description = "Drive 50 km in a single day",
                icon = "🚗",
                targetValue = 50000.0
            ),
            LocationChallengeDefinition(
                key = "drive_200km_day",
                name = "Road Tripper",
                description = "Drive 200 km in a single day",
                icon = "🛣️",
                targetValue = 200000.0
            ),
            LocationChallengeDefinition(
                key = "visit_10_bookmarks",
                name = "Bookmark Explorer",
                description = "Visit 10 different bookmarks",
                icon = "📍",
                targetValue = 10.0
            ),
            LocationChallengeDefinition(
                key = "100_tracks",
                name = "Track Star",
                description = "Record 100 tracks",
                icon = "🎯",
                targetValue = 100.0
            ),
            LocationChallengeDefinition(
                key = "500_photo_waypoints",
                name = "Photo Enthusiast",
                description = "Take 500 photos at waypoints",
                icon = "📷",
                targetValue = 500.0
            ),
            LocationChallengeDefinition(
                key = "10_achievements",
                name = "Achievement Hunter",
                description = "Unlock 10 achievements",
                icon = "🏅",
                targetValue = 10.0
            )
        )
    }

    /**
     * Initialize default challenges if none exist.
     */
    suspend fun initializeChallenges() {
        if (dao.getActiveChallenges().first().isEmpty()) {
            val definitions = getDefaultChallenges()
            for (def in definitions) {
                dao.insert(
                    LocationChallengeEntity(
                        challengeKey = def.key,
                        name = def.name,
                        description = def.description,
                        icon = def.icon,
                        targetValue = def.targetValue
                    )
                )
            }
        }
    }

    /**
     * Update progress on a challenge.
     */
    suspend fun updateProgress(key: String, newValue: Double): Boolean {
        val challenge = dao.getByKey(key) ?: return false
        val completed = newValue >= challenge.targetValue
        dao.updateProgress(key, newValue, completed, if (completed) System.currentTimeMillis() else 0L)
        return completed
    }

    /**
     * Reset an active challenge.
     */
    suspend fun resetChallenge(key: String) {
        dao.updateProgress(key, 0.0, false, 0L)
    }

    /**
     * Get the number of completed challenges.
     */
    suspend fun getCompletedCount(): Int {
        return dao.getCompletedChallenges().first().size
    }

    data class LocationChallengeDefinition(
        val key: String,
        val name: String,
        val description: String,
        val icon: String,
        val targetValue: Double
    )
}
