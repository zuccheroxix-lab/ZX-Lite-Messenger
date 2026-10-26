package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query("SELECT * FROM friends ORDER BY displayName ASC")
    fun observeAllFriends(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    suspend fun getFriendById(id: String): FriendEntity?

    @Query("SELECT * FROM friends WHERE username = :username LIMIT 1")
    suspend fun getFriendByUsername(username: String): FriendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(friend: FriendEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(friends: List<FriendEntity>)

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM friends")
    suspend fun clearAll()
}
