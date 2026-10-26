package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "friends",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["displayName"])
    ]
)
data class FriendEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val friendsSince: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false
)
