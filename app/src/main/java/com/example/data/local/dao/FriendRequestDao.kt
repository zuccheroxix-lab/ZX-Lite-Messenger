package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FriendRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendRequestDao {

    @Query("SELECT * FROM friend_requests WHERE type = 'INCOMING' AND status = 'PENDING' ORDER BY createdAt DESC")
    fun observeIncomingRequests(): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE type = 'OUTGOING' AND status = 'PENDING' ORDER BY createdAt DESC")
    fun observeOutgoingRequests(): Flow<List<FriendRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(request: FriendRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<FriendRequestEntity>)

    @Query("UPDATE friend_requests SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM friend_requests WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM friend_requests WHERE type = :type")
    suspend fun clearByType(type: String)

    @Query("DELETE FROM friend_requests")
    suspend fun clearAll()
}
