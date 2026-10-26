package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "offline_queue",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class OfflineQueueEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val content: String,
    val idempotencyKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
