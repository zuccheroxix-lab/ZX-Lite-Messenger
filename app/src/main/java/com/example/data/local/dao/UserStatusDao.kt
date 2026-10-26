package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.UserStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatusDao {

    @Query("SELECT * FROM user_statuses WHERE expiresAt > :now ORDER BY createdAt DESC")
    fun observeActiveStatuses(now: Long = System.currentTimeMillis()): Flow<List<UserStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(status: UserStatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(statuses: List<UserStatusEntity>)

    @Query("DELETE FROM user_statuses WHERE expiresAt <= :now")
    suspend fun cleanExpired(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_statuses")
    suspend fun clearAll()
}
