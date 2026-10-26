package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["peerUserId"])
    ]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isDirect: Boolean = true,
    val peerUserId: String? = null,
    val peerUsername: String? = null,
    val peerDisplayName: String? = null,
    val lastMessageText: String? = null,
    val lastMessageTimestamp: Long = 0L,
    val lastMessageSenderId: String? = null,
    val unreadCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
