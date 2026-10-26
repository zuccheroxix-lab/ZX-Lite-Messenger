package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE peerUserId = :peerUserId LIMIT 1")
    suspend fun getDirectConversationWith(peerUserId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversations: List<ConversationEntity>)

    @Query("""
        UPDATE conversations 
        SET lastMessageText = :text, 
            lastMessageTimestamp = :timestamp, 
            lastMessageSenderId = :senderId, 
            updatedAt = :timestamp 
        WHERE id = :id
    """)
    suspend fun updateLastMessage(id: String, text: String, timestamp: Long, senderId: String)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun resetUnreadCount(id: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAll()
}
