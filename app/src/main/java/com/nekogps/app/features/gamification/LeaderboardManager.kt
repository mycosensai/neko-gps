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
        totalDistanceMeters: Double,
        tracksRecorded: Int,
        bookmarksCreated: Int,
        achievementsUnlocked: Int,
        photoWaypoints: Int,
        challengesCompleted: Int
    ) {
        val existing = dao.getUserEntry()
        val entry = LeaderboardEntryEntity(
            id = existing?.id ?: 0,
            playerName = playerName,
            isUser = true,
            totalDistanceMeters = totalDistanceMeters,
            tracksRecorded = tracksRecorded,
            bookmarksCreated = bookmarksCreated,
            achievementsUnlocked = achievementsUnlocked,
            photoWaypoints = photoWaypoints,
            challengesCompleted = challengesCompleted
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
        return if (meters >= 1000) {
            String.format("%.1f km", meters / 1000)
        } else {
            String.format("%.0f m", meters)
        }
    }
}
