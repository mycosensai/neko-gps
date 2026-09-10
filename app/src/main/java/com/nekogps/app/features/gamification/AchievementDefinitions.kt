package com.nekogps.app.features.gamification

/**
 * AchievementDefinitions - static catalogue of all available achievements,
 * grouped by category.
 */
object AchievementDefinitions {
    fun distanceDefinitions(): List<AchievementManager.AchievementDefinition> {
        return listOf(
            AchievementManager.AchievementDefinition(
                key = "first_km",
                name = "First Kilometer",
                description = "Travel your first 1 km",
                icon = "🚀"
            ),
            AchievementManager.AchievementDefinition(
                key = "first_10km",
                name = "Ten Klicks",
                description = "Travel 10 km total",
                icon = "🏃"
            ),
            AchievementManager.AchievementDefinition(
                key = "first_100km",
                name = "Century Club",
                description = "Travel 100 km total",
                icon = "🏆"
            ),
            AchievementManager.AchievementDefinition(
                key = "first_1000km",
                name = "Kilometer King",
                description = "Travel 1000 km total",
                icon = "👑"
            )
        )
    }

    fun bookmarkDefinitions(): List<AchievementManager.AchievementDefinition> {
        return listOf(
            AchievementManager.AchievementDefinition(
                key = "first_bookmark",
                name = "Bookworm",
                description = "Create your first bookmark",
                icon = "🔖"
            ),
            AchievementManager.AchievementDefinition(
                key = "ten_bookmarks",
                name = "Bookmark Collector",
                description = "Create 10 bookmarks",
                icon = "📚"
            )
        )
    }

    fun trackDefinitions(): List<AchievementManager.AchievementDefinition> {
        return listOf(
            AchievementManager.AchievementDefinition(
                key = "first_track",
                name = "Pathfinder",
                description = "Record your first track",
                icon = "🗺️"
            ),
            AchievementManager.AchievementDefinition(
                key = "five_tracks",
                name = "Explorer",
                description = "Record 5 tracks",
                icon = "🌍"
            )
        )
    }

    fun photoDefinitions(): List<AchievementManager.AchievementDefinition> {
        return listOf(
            AchievementManager.AchievementDefinition(
                key = "first_photo_waypoint",
                name = "Photographer",
                description = "Take your first photo waypoint",
                icon = "📸"
            ),
            AchievementManager.AchievementDefinition(
                key = "five_photo_waypoints",
                name = "Photo Album",
                description = "Take 5 photo waypoints",
                icon = "🎞️"
            )
        )
    }

    fun challengeDefinitions(): List<AchievementManager.AchievementDefinition> {
        return listOf(
            AchievementManager.AchievementDefinition(
                key = "first_challenge",
                name = "Challenge Accepted",
                description = "Complete your first location challenge",
                icon = "✅"
            ),
            AchievementManager.AchievementDefinition(
                key = "three_challenges",
                name = "Challenge Master",
                description = "Complete 3 location challenges",
                icon = "💪"
            ),
            AchievementManager.AchievementDefinition(
                key = "first_navigation",
                name = "Navigator",
                description = "Complete your first navigation",
                icon = "🧭"
            ),
            AchievementManager.AchievementDefinition(
                key = "speed_demon",
                name = "Speed Demon",
                description = "Reach a max speed of 150 km/h",
                icon = "💨"
            )
        )
    }

    fun all(): List<AchievementManager.AchievementDefinition> {
        return distanceDefinitions() +
            bookmarkDefinitions() +
            trackDefinitions() +
            photoDefinitions() +
            challengeDefinitions()
    }
}
