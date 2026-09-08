package com.nekogps.app.features.gamification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard_entries ORDER BY totalDistanceMeters DESC")
    fun getAllEntries(): Flow<List<LeaderboardEntryEntity>>

    @Query("SELECT * FROM leaderboard_entries WHERE isUser = 1")
    suspend fun getUserEntry(): LeaderboardEntryEntity?

    @Query("SELECT * FROM leaderboard_entries ORDER BY totalDistanceMeters DESC LIMIT 50")
    suspend fun getTopEntries(): List<LeaderboardEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LeaderboardEntryEntity): Long

    @Update
    suspend fun update(entry: LeaderboardEntryEntity)

    @Query("DELETE FROM leaderboard_entries WHERE isUser = 0")
    suspend fun deleteFriends()

    @Query("DELETE FROM leaderboard_entries WHERE playerName = :name")
    suspend fun deleteFriend(name: String)

    @Query("DELETE FROM leaderboard_entries")
    suspend fun deleteAll()
}
