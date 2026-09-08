package com.nekogps.app.features.gamification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY unlocked DESC, id ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE achievementKey = :key")
    suspend fun getByKey(key: String): AchievementEntity?

    @Query("SELECT * FROM achievements WHERE unlocked = 1 ORDER BY id ASC")
    suspend fun getUnlocked(): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE unlocked = 0 ORDER BY id ASC")
    suspend fun getLocked(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: AchievementEntity): Long

    @Update
    suspend fun update(achievement: AchievementEntity)

    @Query("UPDATE achievements SET unlocked = 1, unlockedAt = :time WHERE achievementKey = :key")
    suspend fun unlock(key: String, time: Long)

    @Query("DELETE FROM achievements")
    suspend fun deleteAll()
}
