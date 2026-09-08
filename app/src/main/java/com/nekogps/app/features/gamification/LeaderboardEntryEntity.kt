package com.nekogps.app.features.gamification

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leaderboard_entries")
data class LeaderboardEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerName: String,
    val isUser: Boolean = false,
    val totalDistanceMeters: Double = 0.0,
    val tracksRecorded: Int = 0,
    val bookmarksCreated: Int = 0,
    val achievementsUnlocked: Int = 0,
    val photoWaypoints: Int = 0,
    val challengesCompleted: Int = 0
)
