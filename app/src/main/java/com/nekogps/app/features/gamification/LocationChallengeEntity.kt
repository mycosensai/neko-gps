package com.nekogps.app.features.gamification

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_challenges")
data class LocationChallengeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val challengeKey: String,
    val name: String,
    val description: String,
    val icon: String,
    val targetValue: Double,
    val currentValue: Double = 0.0,
    val completed: Boolean = false,
    val completedAt: Long = 0L
)
