package com.nekogps.app.features.gamification

import android.content.Context
import com.nekogps.app.features.bookmarks.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * LeaderboardManager - simple local leaderboard tracking distance traveled, tracks recorded, etc.
 * Supports friend entries (manually added).
 */
class LeaderboardManager(context: Context) {
    companion object {
        private const val METERS_PER_KILOMETER = 1000
    }

    data class UserStatsUpdate(
        val totalDistanceMeters: Double,
        val tracksRecorded: Int,
        val bookmarksCreated: Int,
        val achievementsUnlocked: Int,
        val photoWaypoints: Int,
        val challengesCompleted: Int
    )

    private val dao = AppDatabase.getInstance(context).leaderboardDao()

    val allEntries: Flow<List<LeaderboardEntryEntity>> = dao.getAllEntries()

    /**
     * Get the user's own entry.
     */
    suspend fun getUserEntry(): LeaderboardEntryEntity? {
        return dao.getUserEntry()
    }

    /**
     * Get or create the user entry, updating stats.
     */
    suspend fun updateUserStats(
        playerName: String = "Neko",
        stats: UserStatsUpdate
    ) {
        val existing = dao.getUserEntry()
        val entry = LeaderboardEntryEntity(
            id = existing?.id ?: 0,
            playerName = playerName,
            isUser = true,
            totalDistanceMeters = stats.totalDistanceMeters,
            tracksRecorded = stats.tracksRecorded,
            bookmarksCreated = stats.bookmarksCreated,
            achievementsUnlocked = stats.achievementsUnlocked,
            photoWaypoints = stats.photoWaypoints,
            challengesCompleted = stats.challengesCompleted
        )
        if (existing != null) {
            dao.update(entry)
        } else {
            dao.insert(entry)
        }
    }

    /**
     * Add or update a friend entry.
     */
    suspend fun addFriend(playerName: String, totalDistanceMeters: Double, tracksRecorded: Int) {
        val all = dao.getTopEntries()
        val existing = all.find { it.playerName == playerName }
        if (existing != null) {
            val entry = LeaderboardEntryEntity(
                id = existing.id,
                playerName = playerName,
                isUser = false,
                totalDistanceMeters = totalDistanceMeters,
                tracksRecorded = tracksRecorded
            )
            dao.update(entry)
        } else {
            dao.insert(
                LeaderboardEntryEntity(
                    playerName = playerName,
                    isUser = false,
                    totalDistanceMeters = totalDistanceMeters,
                    tracksRecorded = tracksRecorded
                )
            )
        }
    }

    /**
     * Manually add a friend.
     */
    suspend fun addFriendManual(playerName: String) {
        val all = dao.getTopEntries()
        val existing = all.find { it.playerName == playerName }
        if (existing == null) {
            dao.insert(
                LeaderboardEntryEntity(
                    playerName = playerName,
                    isUser = false,
                    totalDistanceMeters = 0.0,
                    tracksRecorded = 0
                )
            )
        }
    }

    /**
     * Remove a single friend entry by name.
     */
    suspend fun removeFriend(name: String) {
        dao.deleteFriend(name)
    }

    /**
     * Remove all friend entries.
     */
    suspend fun removeFriends() {
        dao.deleteFriends()
    }

    /**
     * Get top 50 entries sorted by distance.
     */
    suspend fun getTopEntries(): List<LeaderboardEntryEntity> {
        return dao.getTopEntries()
    }

    /**
     * Get the rank of a specific entry.
     */
    suspend fun getRank(): Int {
        val top = dao.getTopEntries()
        val user = dao.getUserEntry() ?: return -1
        return top.indexOfFirst { it.id == user.id } + 1
    }

    /**
     * Format distance for display.
     */
    fun getDistanceString(meters: Double): String {
        return if (meters >= METERS_PER_KILOMETER) {
            String.format(java.util.Locale.getDefault(), "%.1f km", meters / METERS_PER_KILOMETER)
        } else {
            String.format(java.util.Locale.getDefault(), "%.0f m", meters)
        }
    }
}
