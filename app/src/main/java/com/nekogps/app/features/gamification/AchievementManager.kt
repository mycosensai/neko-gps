package com.nekogps.app.features.gamification

import android.content.Context
import com.nekogps.app.features.bookmarks.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * AchievementManager - tracks user achievements and milestones.
 * Milestones: first 1km, 100km, 1000km, first bookmark, first track, etc.
 */
class AchievementManager(context: Context) {
    private val dao = AppDatabase.getInstance(context).achievementDao()

    val allAchievements: Flow<List<AchievementEntity>> = dao.getAllAchievements()

    companion object {
        private const val METERS_PER_KILOMETER = 1000.0
        private const val FIRST_KM_THRESHOLD = 1.0
        private const val TEN_KM_THRESHOLD = 10.0
        private const val HUNDRED_KM_THRESHOLD = 100.0
        private const val THOUSAND_KM_THRESHOLD = 1000.0
        private const val TEN_BOOKMARKS_THRESHOLD = 10
        private const val FIVE_TRACKS_THRESHOLD = 5
        private const val FIVE_PHOTOS_THRESHOLD = 5
        private const val THREE_CHALLENGES_THRESHOLD = 3
    }

    /**
     * Defines all available achievements with their key, name, description, and icon.
     */
    fun getAllAchievementDefinitions(): List<AchievementDefinition> {
        return AchievementDefinitions.all()
    }

    /**
     * Unlock an achievement if it exists and isn't already unlocked.
     * Returns true if the achievement was unlocked.
     */
    suspend fun unlockAchievement(key: String, name: String, description: String, icon: String): Boolean {
        val existing = dao.getByKey(key)
        if (existing != null && existing.unlocked) return false

        val entity = AchievementEntity(
            achievementKey = key,
            name = name,
            description = description,
            icon = icon,
            unlocked = true,
            unlockedAt = System.currentTimeMillis()
        )
        dao.insert(entity)
        return true
    }

    /**
     * Check and unlock milestone achievements based on total distance.
     */
    suspend fun checkDistanceMilestones(totalDistanceMeters: Double): List<String> {
        val unlocked = mutableListOf<String>()
        val distanceKm = totalDistanceMeters / METERS_PER_KILOMETER

        val definitions = getAllAchievementDefinitions()

        val thresholds = mapOf(
            "first_km" to FIRST_KM_THRESHOLD,
            "first_10km" to TEN_KM_THRESHOLD,
            "first_100km" to HUNDRED_KM_THRESHOLD,
            "first_1000km" to THOUSAND_KM_THRESHOLD
        )

        for ((key, thresholdKm) in thresholds) {
            if (distanceKm < thresholdKm) continue
            unlockMilestoneIfDefined(key, definitions, unlocked)
        }
        return unlocked
    }

    /**
     * Check and unlock count-based achievements.
     */
    suspend fun checkCountMilestones(count: Int, category: String): List<String> {
        val unlocked = mutableListOf<String>()
        val definitions = getAllAchievementDefinitions()

        val criteria = when (category) {
            "bookmark" -> listOf(
                "first_bookmark" to 1,
                "ten_bookmarks" to TEN_BOOKMARKS_THRESHOLD
            )
            "track" -> listOf("first_track" to 1, "five_tracks" to FIVE_TRACKS_THRESHOLD)
            "photo_waypoint" -> listOf(
                "first_photo_waypoint" to 1,
                "five_photo_waypoints" to FIVE_PHOTOS_THRESHOLD
            )
            "challenge" -> listOf(
                "first_challenge" to 1,
                "three_challenges" to THREE_CHALLENGES_THRESHOLD
            )
            else -> emptyList()
        }

        for ((key, threshold) in criteria) {
            if (count < threshold) continue
            unlockMilestoneIfDefined(key, definitions, unlocked)
        }
        return unlocked
    }

    private suspend fun unlockMilestoneIfDefined(
        key: String,
        definitions: List<AchievementDefinition>,
        unlocked: MutableList<String>
    ) {
        val def = definitions.find { it.key == key } ?: return
        if (unlockAchievement(key, def.name, def.description, def.icon)) {
            unlocked.add(key)
        }
    }

    suspend fun getUnlockedCount(): Int {
        return dao.getUnlocked().size
    }

    data class AchievementDefinition(
        val key: String,
        val name: String,
        val description: String,
        val icon: String
    )
}
