package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "username") val username: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "password") val password: String,
    @Json(name = "phoneNumber") val phoneNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RefreshRequest(
    @Json(name = "refreshToken") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null,
    @Json(name = "phoneVerified") val phoneVerified: Boolean = false,
    @Json(name = "createdAt") val createdAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String,
    @Json(name = "user") val user: UserDto
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateDirectConversationRequest(
    @Json(name = "peerUsername") val peerUsername: String
)

@JsonClass(generateAdapter = true)
data class ConversationDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "isDirect") val isDirect: Boolean = true,
    @Json(name = "peerUser") val peerUser: UserDto? = null,
    @Json(name = "lastMessage") val lastMessage: MessageDto? = null,
    @Json(name = "unreadCount") val unreadCount: Int = 0,
    @Json(name = "updatedAt") val updatedAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @Json(name = "content") val content: String,
    @Json(name = "idempotencyKey") val idempotencyKey: String
)

@JsonClass(generateAdapter = true)
data class MessageDto(
    @Json(name = "id") val id: String,
    @Json(name = "conversationId") val conversationId: String,
    @Json(name = "senderId") val senderId: String,
    @Json(name = "senderUsername") val senderUsername: String,
    @Json(name = "recipientId") val recipientId: String? = null,
    @Json(name = "content") val content: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "status") val status: String = "SENT",
    @Json(name = "idempotencyKey") val idempotencyKey: String = ""
)

@JsonClass(generateAdapter = true)
data class MessagesResponse(
    @Json(name = "messages") val messages: List<MessageDto>,
    @Json(name = "nextCursor") val nextCursor: String? = null,
    @Json(name = "hasMore") val hasMore: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MarkReadRequest(
    @Json(name = "conversationId") val conversationId: String
)

// Friends & Friend Requests DTOs
@JsonClass(generateAdapter = true)
data class FriendDto(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "friendsSince") val friendsSince: Long = 0L,
    @Json(name = "isOnline") val isOnline: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SendFriendRequest(
    @Json(name = "targetUsername") val targetUsername: String? = null,
    @Json(name = "targetPhoneNumber") val targetPhoneNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class RequestIdPayload(
    @Json(name = "requestId") val requestId: String
)

@JsonClass(generateAdapter = true)
data class IncomingFriendRequestDto(
    @Json(name = "id") val id: String,
    @Json(name = "createdAt") val createdAt: Long,
    @Json(name = "sender") val sender: UserDto
)

@JsonClass(generateAdapter = true)
data class OutgoingFriendRequestDto(
    @Json(name = "id") val id: String,
    @Json(name = "createdAt") val createdAt: Long,
    @Json(name = "receiver") val receiver: UserDto
)

// Contact Discovery DTOs
@JsonClass(generateAdapter = true)
data class ContactDiscoveryRequest(
    @Json(name = "phoneNumbers") val phoneNumbers: List<String>
)

@JsonClass(generateAdapter = true)
data class ContactMatchDto(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null,
    @Json(name = "isFriend") val isFriend: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ContactDiscoveryResponse(
    @Json(name = "matches") val matches: List<ContactMatchDto>
)

// Privacy Settings DTOs
@JsonClass(generateAdapter = true)
data class PrivacySettingsDto(
    @Json(name = "autoAcceptMatchedContacts") val autoAcceptMatchedContacts: Boolean = false,
    @Json(name = "allowPhoneDiscovery") val allowPhoneDiscovery: Boolean = true,
    @Json(name = "readReceiptsEnabled") val readReceiptsEnabled: Boolean = true
)

// Blocked Users DTOs
@JsonClass(generateAdapter = true)
data class BlockedUserDto(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "blockedAt") val blockedAt: Long = 0L
)

@JsonClass(generateAdapter = true)
data class BlockUserPayload(
    @Json(name = "targetUserId") val targetUserId: String
)

// Status / Updates DTOs
@JsonClass(generateAdapter = true)
data class UserStatusDto(
    @Json(name = "id") val id: String,
    @Json(name = "text") val text: String,
    @Json(name = "createdAt") val createdAt: Long,
    @Json(name = "expiresAt") val expiresAt: Long,
    @Json(name = "user") val user: UserDto
)

@JsonClass(generateAdapter = true)
data class CreateStatusRequest(
    @Json(name = "text") val text: String
)

// Profile Update DTO
@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null
)

// FCM Token DTO
@JsonClass(generateAdapter = true)
data class FcmTokenPayload(
    @Json(name = "token") val token: String,
    @Json(name = "deviceId") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "data") val data: T? = null,
    @Json(name = "error") val error: String? = null
)
