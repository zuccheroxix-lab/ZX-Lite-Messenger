package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "friend_requests",
    indices = [
        Index(value = ["type"]),
        Index(value = ["status"]),
        Index(value = ["peerUsername"])
    ]
)
data class FriendRequestEntity(
    @PrimaryKey val id: String,
    val type: String, // "INCOMING" or "OUTGOING"
    val peerId: String,
    val peerUsername: String,
    val peerDisplayName: String,
    val peerAvatarUrl: String? = null,
    val peerBio: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // "PENDING", "ACCEPTED", "DECLINED", "CANCELLED"
)
