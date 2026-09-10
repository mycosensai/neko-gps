package com.nekogps.app.features.gamification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationChallengeDao {
    @Query("SELECT * FROM location_challenges ORDER BY completed ASC, id ASC")
    fun getAllChallenges(): Flow<List<LocationChallengeEntity>>

    @Query("SELECT * FROM location_challenges WHERE completed = 0 ORDER BY id ASC")
    fun getActiveChallenges(): Flow<List<LocationChallengeEntity>>

    @Query("SELECT * FROM location_challenges WHERE completed = 1 ORDER BY id ASC")
    fun getCompletedChallenges(): Flow<List<LocationChallengeEntity>>

    @Query("SELECT * FROM location_challenges WHERE challengeKey = :key")
    suspend fun getByKey(key: String): LocationChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(challenge: LocationChallengeEntity): Long

    @Update
    suspend fun update(challenge: LocationChallengeEntity)

    @Query(
        "UPDATE location_challenges SET currentValue = :value, " +
            "completed = :completed, completedAt = :time WHERE challengeKey = :key"
    )
    suspend fun updateProgress(key: String, value: Double, completed: Boolean, time: Long)

    @Query("DELETE FROM location_challenges")
    suspend fun deleteAll()
}
