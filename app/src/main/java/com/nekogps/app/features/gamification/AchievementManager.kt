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

    /**
     * Defines all available achievements with their key, name, description, and icon.
     */
    fun getAllAchievementDefinitions(): List<AchievementDefinition> {
        return listOf(
            AchievementDefinition(
                key = "first_km",
                name = "First Kilometer",
                description = "Travel your first 1 km",
                icon = "🚀"
            ),
            AchievementDefinition(
                key = "first_10km",
                name = "Ten Klicks",
                description = "Travel 10 km total",
                icon = "🏃"
            ),
            AchievementDefinition(
                key = "first_100km",
                name = "Century Club",
                description = "Travel 100 km total",
                icon = "🏆"
            ),
            AchievementDefinition(
                key = "first_1000km",
                name = "Kilometer King",
                description = "Travel 1000 km total",
                icon = "👑"
            ),
            AchievementDefinition(
                key = "first_bookmark",
                name = "Bookworm",
                description = "Create your first bookmark",
                icon = "🔖"
            ),
            AchievementDefinition(
                key = "ten_bookmarks",
                name = "Bookmark Collector",
                description = "Create 10 bookmarks",
                icon = "📚"
            ),
            AchievementDefinition(
                key = "first_track",
                name = "Pathfinder",
                description = "Record your first track",
                icon = "🗺️"
            ),
            AchievementDefinition(
                key = "five_tracks",
                name = "Explorer",
                description = "Record 5 tracks",
                icon = "🌍"
            ),
            AchievementDefinition(
                key = "first_photo_waypoint",
                name = "Photographer",
                description = "Take your first photo waypoint",
                icon = "📸"
            ),
            AchievementDefinition(
                key = "five_photo_waypoints",
                name = "Photo Album",
                description = "Take 5 photo waypoints",
                icon = "🎞️"
            ),
            AchievementDefinition(
                key = "first_challenge",
                name = "Challenge Accepted",
                description = "Complete your first location challenge",
                icon = "✅"
            ),
            AchievementDefinition(
                key = "three_challenges",
                name = "Challenge Master",
                description = "Complete 3 location challenges",
                icon = "💪"
            ),
            AchievementDefinition(
                key = "first_navigation",
                name = "Navigator",
                description = "Complete your first navigation",
                icon = "🧭"
            ),
            AchievementDefinition(
                key = "speed_demon",
                name = "Speed Demon",
                description = "Reach a max speed of 150 km/h",
                icon = "💨"
            )
        )
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
        val distanceKm = totalDistanceMeters / 1000.0

        val definitions = getAllAchievementDefinitions()

        val thresholds = mapOf(
            "first_km" to 1.0,
            "first_10km" to 10.0,
            "first_100km" to 100.0,
            "first_1000km" to 1000.0
        )

        for ((key, thresholdKm) in thresholds) {
            if (distanceKm >= thresholdKm) {
                val def = definitions.find { it.key == key }
                if (def != null) {
                    val wasUnlocked = unlockAchievement(key, def.name, def.description, def.icon)
                    if (wasUnlocked) unlocked.add(key)
                }
            }
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
            "bookmark" -> listOf("first_bookmark" to 1, "ten_bookmarks" to 10)
            "track" -> listOf("first_track" to 1, "five_tracks" to 5)
            "photo_waypoint" -> listOf("first_photo_waypoint" to 1, "five_photo_waypoints" to 5)
            "challenge" -> listOf("first_challenge" to 1, "three_challenges" to 3)
            else -> emptyList()
        }

        for ((key, threshold) in criteria) {
            if (count >= threshold) {
                val def = definitions.find { it.key == key }
                if (def != null) {
                    val wasUnlocked = unlockAchievement(key, def.name, def.description, def.icon)
                    if (wasUnlocked) unlocked.add(key)
                }
            }
        }
        return unlocked
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
