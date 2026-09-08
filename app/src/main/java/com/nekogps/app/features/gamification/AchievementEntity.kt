package com.nekogps.app.features.gamification

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val achievementKey: String,
    val name: String,
    val description: String,
    val icon: String,
    val unlocked: Boolean = false,
    val unlockedAt: Long = 0L
)
