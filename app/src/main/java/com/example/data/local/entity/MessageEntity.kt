package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderUsername: String,
    val recipientId: String? = null,
    val content: String,
    val timestamp: Long,
    val status: String = STATUS_SENT, // PENDING, SENT, DELIVERED, READ
    val isOutgoing: Boolean,
    val idempotencyKey: String
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENT = "SENT"
        const val STATUS_DELIVERED = "DELIVERED"
        const val STATUS_READ = "READ"
    }
}
