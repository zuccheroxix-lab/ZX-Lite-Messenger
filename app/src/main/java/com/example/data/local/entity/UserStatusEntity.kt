package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_statuses",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["expiresAt"])
    ]
)
data class UserStatusEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val text: String,
    val createdAt: Long,
    val expiresAt: Long
)
