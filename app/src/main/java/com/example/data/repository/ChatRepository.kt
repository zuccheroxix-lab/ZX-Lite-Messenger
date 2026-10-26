package com.example.data.repository

import android.util.Log
import com.example.core.preferences.UserPreferencesManager
import com.example.core.security.SecureSessionManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BlockedUserEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.FriendEntity
import com.example.data.local.entity.FriendRequestEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.OfflineQueueEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserStatusEntity
import com.example.data.remote.NetworkClient
import com.example.data.remote.model.BlockUserPayload
import com.example.data.remote.model.ContactDiscoveryRequest
import com.example.data.remote.model.ContactMatchDto
import com.example.data.remote.model.CreateDirectConversationRequest
import com.example.data.remote.model.CreateStatusRequest
import com.example.data.remote.model.LoginRequest
import com.example.data.remote.model.PrivacySettingsDto
import com.example.data.remote.model.RegisterRequest
import com.example.data.remote.model.RequestIdPayload
import com.example.data.remote.model.SendFriendRequest
import com.example.data.remote.model.SendMessageRequest
import com.example.data.remote.model.UpdateProfileRequest
import com.example.data.remote.model.UserDto
import com.example.data.remote.websocket.ChatWebSocketClient
import com.example.data.remote.websocket.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val database: AppDatabase,
    private val networkClient: NetworkClient,
    val sessionManager: SecureSessionManager,
    val preferencesManager: UserPreferencesManager,
    val wsClient: ChatWebSocketClient
) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val offlineQueueDao = database.offlineQueueDao()
    private val userDao = database.userDao()
    private val friendDao = database.friendDao()
    private val friendRequestDao = database.friendRequestDao()
    private val blockedUserDao = database.blockedUserDao()
    private val userStatusDao = database.userStatusDao()

    val connectionState: StateFlow<ConnectionState> = wsClient.connectionState

    init {
        // Collect incoming messages
        scope.launch {
            wsClient.incomingMessages.collect { dto ->
                val myUserId = sessionManager.getUserId() ?: ""
                val isOutgoing = dto.senderId == myUserId
                val entity = MessageEntity(
                    id = dto.id,
                    conversationId = dto.conversationId,
                    senderId = dto.senderId,
                    senderUsername = dto.senderUsername,
                    recipientId = dto.recipientId,
                    content = dto.content,
                    timestamp = dto.timestamp,
                    status = if (isOutgoing) dto.status else MessageEntity.STATUS_DELIVERED,
                    isOutgoing = isOutgoing,
                    idempotencyKey = dto.idempotencyKey.ifBlank { dto.id }
                )
                messageDao.insertOrReplace(entity)

                val conv = conversationDao.getConversationById(dto.conversationId)
                if (conv != null) {
                    val newUnread = if (isOutgoing) conv.unreadCount else conv.unreadCount + 1
                    conversationDao.insertOrUpdate(
                        conv.copy(
                            lastMessageText = dto.content,
                            lastMessageTimestamp = dto.timestamp,
                            lastMessageSenderId = dto.senderId,
                            unreadCount = newUnread,
                            updatedAt = dto.timestamp
                        )
                    )
                } else {
                    refreshConversations()
                }
            }
        }

        // Collect receipts
        scope.launch {
            wsClient.messageReceipts.collect { receipt ->
                if (!receipt.idempotencyKey.isNullOrBlank()) {
                    messageDao.updateIdAndStatusByIdempotency(
                        receipt.idempotencyKey,
                        receipt.messageId,
                        receipt.status
                    )
                    offlineQueueDao.deleteByIdempotencyKey(receipt.idempotencyKey)
                } else {
                    messageDao.updateStatus(receipt.messageId, receipt.status)
                }
            }
        }

        // Collect real-time friend request events
        scope.launch {
            wsClient.incomingFriendRequests.collect { req ->
                friendRequestDao.insertOrUpdate(
                    FriendRequestEntity(
                        id = req.id,
                        type = "INCOMING",
                        peerId = req.sender.id,
                        peerUsername = req.sender.username,
                        peerDisplayName = req.sender.displayName,
                        peerAvatarUrl = req.sender.avatarUrl,
                        peerBio = req.sender.bio,
                        createdAt = req.createdAt,
                        status = "PENDING"
                    )
                )
            }
        }

        // Collect real-time friend accepted events
        scope.launch {
            wsClient.friendRequestAccepted.collect { user ->
                friendDao.insertOrUpdate(
                    FriendEntity(
                        id = user.id,
                        username = user.username,
                        displayName = user.displayName,
                        avatarUrl = user.avatarUrl,
                        bio = user.bio,
                        friendsSince = System.currentTimeMillis(),
                        isOnline = true
                    )
                )
            }
        }

        // Offline queue sync
        scope.launch {
            wsClient.connectionState.collect { state ->
                if (state == ConnectionState.CONNECTED) {
                    processOfflineQueue()
                }
            }
        }
    }

    // --- Authentication ---

    suspend fun register(username: String, displayName: String, password: String, phoneNumber: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = networkClient.authApi.register(
                    RegisterRequest(
                        username = username.trim().lowercase(),
                        displayName = displayName.trim(),
                        password = password,
                        phoneNumber = phoneNumber?.trim()?.ifEmpty { null }
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    sessionManager.saveSession(
                        accessToken = auth.accessToken,
                        refreshToken = auth.refreshToken,
                        userId = auth.user.id,
                        username = auth.user.username,
                        displayName = auth.user.displayName
                    )
                    userDao.insertOrUpdate(
                        UserEntity(
                            id = auth.user.id,
                            username = auth.user.username,
                            displayName = auth.user.displayName
                        )
                    )
                    wsClient.connect()
                    Result.success(Unit)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed (${response.code()})"
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                if (preferencesManager.isDemoGatewayEnabled() || e.message?.contains("Failed to connect") == true) {
                    val demoId = UUID.randomUUID().toString()
                    val cleanUsername = username.trim().lowercase()
                    sessionManager.saveSession(
                        accessToken = "demo_token_$demoId",
                        refreshToken = "demo_refresh_$demoId",
                        userId = demoId,
                        username = cleanUsername,
                        displayName = displayName.ifBlank { cleanUsername }
                    )
                    userDao.insertOrUpdate(
                        UserEntity(
                            id = demoId,
                            username = cleanUsername,
                            displayName = displayName.ifBlank { cleanUsername }
                        )
                    )
                    seedDemoData(demoId, cleanUsername)
                    return@withContext Result.success(Unit)
                }
                Result.failure(e)
            }
        }

    suspend fun login(username: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = networkClient.authApi.login(
                    LoginRequest(username = username.trim().lowercase(), password = password)
                )
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    sessionManager.saveSession(
                        accessToken = auth.accessToken,
                        refreshToken = auth.refreshToken,
                        userId = auth.user.id,
                        username = auth.user.username,
                        displayName = auth.user.displayName
                    )
                    userDao.insertOrUpdate(
                        UserEntity(
                            id = auth.user.id,
                            username = auth.user.username,
                            displayName = auth.user.displayName
                        )
                    )
                    wsClient.connect()
                    refreshConversations()
                    refreshFriends()
                    Result.success(Unit)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Login failed (${response.code()})"
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                if (preferencesManager.isDemoGatewayEnabled() || e.message?.contains("Failed to connect") == true) {
                    val demoId = "demo_user_" + username.hashCode().toString().takeLast(6)
                    val cleanUsername = username.trim().lowercase()
                    sessionManager.saveSession(
                        accessToken = "demo_token_$demoId",
                        refreshToken = "demo_refresh_$demoId",
                        userId = demoId,
                        username = cleanUsername,
                        displayName = cleanUsername.replaceFirstChar { it.uppercase() }
                    )
                    userDao.insertOrUpdate(
                        UserEntity(
                            id = demoId,
                            username = cleanUsername,
                            displayName = cleanUsername.replaceFirstChar { it.uppercase() }
                        )
                    )
                    seedDemoData(demoId, cleanUsername)
                    return@withContext Result.success(Unit)
                }
                Result.failure(e)
            }
        }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            networkClient.userApi.clearFcmToken()
            networkClient.authApi.logout()
        } catch (_: Exception) {}
        wsClient.disconnect()
        sessionManager.clearSession()
        conversationDao.clearAll()
        messageDao.clearAll()
        offlineQueueDao.clearAll()
        friendDao.clearAll()
        friendRequestDao.clearAll()
        blockedUserDao.clearAll()
        userStatusDao.clearAll()
    }

    // --- Conversations & Messaging ---

    fun observeConversations(): Flow<List<ConversationEntity>> =
        conversationDao.observeAllConversations()

    suspend fun refreshConversations(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = networkClient.chatApi.getConversations()
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!.map { dto ->
                    ConversationEntity(
                        id = dto.id,
                        title = dto.title ?: dto.peerUser?.displayName ?: "Direct Chat",
                        isDirect = dto.isDirect,
                        peerUserId = dto.peerUser?.id,
                        peerUsername = dto.peerUser?.username,
                        peerDisplayName = dto.peerUser?.displayName,
                        lastMessageText = dto.lastMessage?.content,
                        lastMessageTimestamp = dto.lastMessage?.timestamp ?: dto.updatedAt,
                        lastMessageSenderId = dto.lastMessage?.senderId,
                        unreadCount = dto.unreadCount,
                        updatedAt = dto.updatedAt
                    )
                }
                conversationDao.insertAll(list)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch conversations"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectConversation(peerUsername: String): Result<ConversationEntity> =
        withContext(Dispatchers.IO) {
            try {
                val response = networkClient.chatApi.createDirectConversation(
                    CreateDirectConversationRequest(peerUsername.trim().lowercase())
                )
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    val entity = ConversationEntity(
                        id = dto.id,
                        title = dto.title ?: dto.peerUser?.displayName ?: peerUsername,
                        isDirect = dto.isDirect,
                        peerUserId = dto.peerUser?.id,
                        peerUsername = dto.peerUser?.username ?: peerUsername,
                        peerDisplayName = dto.peerUser?.displayName ?: peerUsername,
                        lastMessageText = dto.lastMessage?.content,
                        lastMessageTimestamp = dto.lastMessage?.timestamp ?: System.currentTimeMillis(),
                        unreadCount = 0,
                        updatedAt = System.currentTimeMillis()
                    )
                    conversationDao.insertOrUpdate(entity)
                    Result.success(entity)
                } else {
                    val err = response.errorBody()?.string() ?: "Failed to open conversation"
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.observeMessages(conversationId)

    suspend fun loadMessages(conversationId: String, cursor: String? = null): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val response = networkClient.chatApi.getMessages(conversationId, cursor, limit = 30)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val myUserId = sessionManager.getUserId() ?: ""
                    val entities = data.messages.map { dto ->
                        val isOutgoing = dto.senderId == myUserId
                        MessageEntity(
                            id = dto.id,
                            conversationId = dto.conversationId,
                            senderId = dto.senderId,
                            senderUsername = dto.senderUsername,
                            recipientId = dto.recipientId,
                            content = dto.content,
                            timestamp = dto.timestamp,
                            status = if (isOutgoing) dto.status else MessageEntity.STATUS_READ,
                            isOutgoing = isOutgoing,
                            idempotencyKey = dto.idempotencyKey.ifBlank { dto.id }
                        )
                    }
                    messageDao.insertAll(entities)
                    Result.success(data.hasMore)
                } else {
                    Result.failure(Exception("Failed to load messages"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun sendMessage(conversationId: String, text: String): Result<MessageEntity> =
        withContext(Dispatchers.IO) {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return@withContext Result.failure(Exception("Message cannot be empty"))

            val myUserId = sessionManager.getUserId() ?: "unknown"
            val myUsername = sessionManager.getUsername() ?: "me"
            val idempotencyKey = UUID.randomUUID().toString()
            val tempMessageId = "msg_local_$idempotencyKey"
            val now = System.currentTimeMillis()

            val localMessage = MessageEntity(
                id = tempMessageId,
                conversationId = conversationId,
                senderId = myUserId,
                senderUsername = myUsername,
                content = trimmed,
                timestamp = now,
                status = MessageEntity.STATUS_PENDING,
                isOutgoing = true,
                idempotencyKey = idempotencyKey
            )
            messageDao.insertOrReplace(localMessage)
            conversationDao.updateLastMessage(conversationId, trimmed, now, myUserId)

            offlineQueueDao.insert(
                OfflineQueueEntity(
                    id = tempMessageId,
                    conversationId = conversationId,
                    content = trimmed,
                    idempotencyKey = idempotencyKey
                )
            )

            val sentOverWs = wsClient.sendMessage(conversationId, trimmed, idempotencyKey)
            if (sentOverWs) {
                return@withContext Result.success(localMessage)
            }

            try {
                val response = networkClient.chatApi.sendMessage(
                    id = conversationId,
                    request = SendMessageRequest(content = trimmed, idempotencyKey = idempotencyKey)
                )
                if (response.isSuccessful && response.body() != null) {
                    val serverMsg = response.body()!!
                    messageDao.updateIdAndStatusByIdempotency(
                        idempotencyKey = idempotencyKey,
                        newId = serverMsg.id,
                        status = MessageEntity.STATUS_SENT
                    )
                    offlineQueueDao.deleteByIdempotencyKey(idempotencyKey)
                    return@withContext Result.success(
                        localMessage.copy(id = serverMsg.id, status = MessageEntity.STATUS_SENT)
                    )
                }
            } catch (e: Exception) {
                Log.w("ChatRepository", "Message queued: ${e.message}")
            }

            Result.success(localMessage)
        }

    suspend fun markConversationRead(conversationId: String) = withContext(Dispatchers.IO) {
        conversationDao.resetUnreadCount(conversationId)
        messageDao.markAllIncomingAsRead(conversationId)
        wsClient.sendReadReceipt(conversationId)
        try {
            networkClient.chatApi.markAsRead(conversationId)
        } catch (_: Exception) {}
    }

    // --- Friends & Friend Requests ---

    fun observeFriends(): Flow<List<FriendEntity>> = friendDao.observeAllFriends()

    suspend fun refreshFriends(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = networkClient.friendApi.getFriends()
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!.map { dto ->
                    FriendEntity(
                        id = dto.id,
                        username = dto.username,
                        displayName = dto.displayName,
                        avatarUrl = dto.avatarUrl,
                        bio = dto.bio,
                        friendsSince = dto.friendsSince,
                        isOnline = dto.isOnline
                    )
                }
                friendDao.insertAll(list)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch friends"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendFriendRequest(targetUsername: String?, targetPhoneNumber: String?): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = networkClient.friendApi.sendFriendRequest(
                    SendFriendRequest(targetUsername = targetUsername, targetPhoneNumber = targetPhoneNumber)
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    val msg = (body?.get("message") as? String) ?: "Request sent"
                    refreshRequests()
                    Result.success(msg)
                } else {
                    val err = response.errorBody()?.string() ?: "Failed to send request"
                    Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun observeIncomingRequests(): Flow<List<FriendRequestEntity>> =
        friendRequestDao.observeIncomingRequests()

    fun observeOutgoingRequests(): Flow<List<FriendRequestEntity>> =
        friendRequestDao.observeOutgoingRequests()

    suspend fun refreshRequests(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val inResp = networkClient.friendApi.getIncomingRequests()
            if (inResp.isSuccessful && inResp.body() != null) {
                friendRequestDao.clearByType("INCOMING")
                val inList = inResp.body()!!.map { r ->
                    FriendRequestEntity(
                        id = r.id,
                        type = "INCOMING",
                        peerId = r.sender.id,
                        peerUsername = r.sender.username,
                        peerDisplayName = r.sender.displayName,
                        peerAvatarUrl = r.sender.avatarUrl,
                        peerBio = r.sender.bio,
                        createdAt = r.createdAt,
                        status = "PENDING"
                    )
                }
                friendRequestDao.insertAll(inList)
            }

            val outResp = networkClient.friendApi.getOutgoingRequests()
            if (outResp.isSuccessful && outResp.body() != null) {
                friendRequestDao.clearByType("OUTGOING")
                val outList = outResp.body()!!.map { r ->
                    FriendRequestEntity(
                        id = r.id,
                        type = "OUTGOING",
                        peerId = r.receiver.id,
                        peerUsername = r.receiver.username,
                        peerDisplayName = r.receiver.displayName,
                        peerAvatarUrl = r.receiver.avatarUrl,
                        peerBio = r.receiver.bio,
                        createdAt = r.createdAt,
                        status = "PENDING"
                    )
                }
                friendRequestDao.insertAll(outList)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = networkClient.friendApi.acceptFriendRequest(RequestIdPayload(requestId))
            if (resp.isSuccessful) {
                friendRequestDao.updateStatus(requestId, "ACCEPTED")
                refreshFriends()
                refreshRequests()
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "Failed to accept"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineFriendRequest(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = networkClient.friendApi.declineFriendRequest(RequestIdPayload(requestId))
            if (resp.isSuccessful) {
                friendRequestDao.updateStatus(requestId, "DECLINED")
                refreshRequests()
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "Failed to decline"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelFriendRequest(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = networkClient.friendApi.cancelFriendRequest(RequestIdPayload(requestId))
            if (resp.isSuccessful) {
                friendRequestDao.updateStatus(requestId, "CANCELLED")
                refreshRequests()
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "Failed to cancel"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfriend(friendUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = networkClient.friendApi.unfriend(friendUserId)
            if (resp.isSuccessful) {
                friendDao.deleteById(friendUserId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "Failed to unfriend"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Contact Discovery ---

    suspend fun discoverContacts(phoneNumbers: List<String>): Result<List<ContactMatchDto>> =
        withContext(Dispatchers.IO) {
            try {
                val resp = networkClient.contactApi.discoverContacts(ContactDiscoveryRequest(phoneNumbers))
                if (resp.isSuccessful && resp.body() != null) {
                    Result.success(resp.body()!!.matches)
                } else {
                    Result.failure(Exception(resp.errorBody()?.string() ?: "Discovery failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // --- Updates / Status ---

    fun observeStatuses(): Flow<List<UserStatusEntity>> = userStatusDao.observeActiveStatuses()

    suspend fun refreshStatuses(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = networkClient.updateApi.getUpdates()
            if (resp.isSuccessful && resp.body() != null) {
                val list = resp.body()!!.map { s ->
                    UserStatusEntity(
                        id = s.id,
                        userId = s.user.id,
                        username = s.user.username,
                        displayName = s.user.displayName,
                        avatarUrl = s.user.avatarUrl,
                        text = s.text,
                        createdAt = s.createdAt,
                        expiresAt = s.expiresAt
                    )
                }
                userStatusDao.insertAll(list)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch updates"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postStatus(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = networkClient.updateApi.postUpdate(CreateStatusRequest(text.trim()))
            if (resp.isSuccessful && resp.body() != null) {
                val s = resp.body()!!
                userStatusDao.insertOrUpdate(
                    UserStatusEntity(
                        id = s.id,
                        userId = s.user.id,
                        username = s.user.username,
                        displayName = s.user.displayName,
                        avatarUrl = s.user.avatarUrl,
                        text = s.text,
                        createdAt = s.createdAt,
                        expiresAt = s.expiresAt
                    )
                )
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "Failed to post status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Blocked Users ---

    fun observeBlockedUsers(): Flow<List<BlockedUserEntity>> = blockedUserDao.observeBlockedUsers()

    suspend fun refreshBlockedUsers(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = networkClient.userApi.getBlockedUsers()
            if (resp.isSuccessful && resp.body() != null) {
                blockedUserDao.clearAll()
                val list = resp.body()!!.map { b ->
                    BlockedUserEntity(
                        id = b.id,
                        username = b.username,
                        displayName = b.displayName,
                        avatarUrl = b.avatarUrl,
                        blockedAt = b.blockedAt
                    )
                }
                blockedUserDao.insertAll(list)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch blocked users"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun blockUser(targetUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = networkClient.userApi.blockUser(BlockUserPayload(targetUserId))
            if (resp.isSuccessful) {
                refreshBlockedUsers()
                refreshFriends()
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "Failed to block user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = networkClient.userApi.unblockUser(BlockUserPayload(targetUserId))
            if (resp.isSuccessful) {
                blockedUserDao.deleteById(targetUserId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "Failed to unblock user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Privacy Settings ---

    suspend fun getPrivacySettings(): Result<PrivacySettingsDto> = withContext(Dispatchers.IO) {
        try {
            val resp = networkClient.userApi.getPrivacySettings()
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("Failed to load privacy settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePrivacySettings(settings: PrivacySettingsDto): Result<PrivacySettingsDto> =
        withContext(Dispatchers.IO) {
            try {
                val resp = networkClient.userApi.updatePrivacySettings(settings)
                if (resp.isSuccessful && resp.body() != null) {
                    Result.success(resp.body()!!)
                } else {
                    Result.failure(Exception("Failed to update privacy settings"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // --- User Profile ---

    suspend fun getMyProfile(): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val resp = networkClient.userApi.getMyProfile()
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("Failed to load profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMyProfile(displayName: String?, bio: String?, phoneNumber: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val resp = networkClient.userApi.updateMyProfile(
                    UpdateProfileRequest(displayName = displayName, bio = bio, phoneNumber = phoneNumber)
                )
                if (resp.isSuccessful) {
                    if (!displayName.isNullOrBlank()) {
                        sessionManager.saveSession(
                            accessToken = sessionManager.getAccessToken() ?: "",
                            refreshToken = sessionManager.getRefreshToken() ?: "",
                            userId = sessionManager.getUserId() ?: "",
                            username = sessionManager.getUsername() ?: "",
                            displayName = displayName
                        )
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to update profile"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun searchUsers(query: String): Result<List<UserDto>> = withContext(Dispatchers.IO) {
        try {
            val response = networkClient.userApi.searchUsers(query.trim())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("No users found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processOfflineQueue() = withContext(Dispatchers.IO) {
        val pending = offlineQueueDao.getAllPending()
        for (item in pending) {
            try {
                val sent = wsClient.sendMessage(item.conversationId, item.content, item.idempotencyKey)
                if (!sent) {
                    val resp = networkClient.chatApi.sendMessage(
                        item.conversationId,
                        SendMessageRequest(content = item.content, idempotencyKey = item.idempotencyKey)
                    )
                    if (resp.isSuccessful && resp.body() != null) {
                        val msg = resp.body()!!
                        messageDao.updateIdAndStatusByIdempotency(
                            item.idempotencyKey,
                            msg.id,
                            MessageEntity.STATUS_SENT
                        )
                        offlineQueueDao.deleteByIdempotencyKey(item.idempotencyKey)
                    } else {
                        offlineQueueDao.incrementRetry(item.id)
                    }
                }
            } catch (e: Exception) {
                offlineQueueDao.incrementRetry(item.id)
            }
        }
    }

    suspend fun clearLocalCache() = withContext(Dispatchers.IO) {
        messageDao.clearAll()
        offlineQueueDao.clearAll()
    }

    private suspend fun seedDemoData(myUserId: String, myUsername: String) {
        val friend1 = FriendEntity(
            id = "u_zx_lead",
            username = "zann",
            displayName = "Zucchero Xann",
            bio = "Lead Architect at ZX Lite",
            friendsSince = System.currentTimeMillis() - 86400000,
            isOnline = true
        )
        friendDao.insertOrUpdate(friend1)

        val conv1 = ConversationEntity(
            id = "conv_zx_official",
            title = "Zucchero Xann",
            isDirect = true,
            peerUserId = "u_zx_lead",
            peerUsername = "zann",
            peerDisplayName = "Zucchero Xann",
            lastMessageText = "Welcome to ZX Lite Messenger. Real-time, low RAM, and secure.",
            lastMessageTimestamp = System.currentTimeMillis() - 60000,
            unreadCount = 0,
            updatedAt = System.currentTimeMillis() - 60000
        )
        conversationDao.insertOrUpdate(conv1)

        val msg1 = MessageEntity(
            id = "msg_welcome_1",
            conversationId = "conv_zx_official",
            senderId = "u_zx_lead",
            senderUsername = "zann",
            recipientId = myUserId,
            content = "Welcome to ZX Lite Messenger. Real-time, low RAM, and secure.",
            timestamp = System.currentTimeMillis() - 60000,
            status = MessageEntity.STATUS_READ,
            isOutgoing = false,
            idempotencyKey = "key_seed_1"
        )
        messageDao.insertOrReplace(msg1)
    }
}
