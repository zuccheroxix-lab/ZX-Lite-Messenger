package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BlockedUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedUserDao {

    @Query("SELECT * FROM blocked_users ORDER BY blockedAt DESC")
    fun observeBlockedUsers(): Flow<List<BlockedUserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(blocked: List<BlockedUserEntity>)

    @Query("DELETE FROM blocked_users WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM blocked_users")
    suspend fun clearAll()
}
