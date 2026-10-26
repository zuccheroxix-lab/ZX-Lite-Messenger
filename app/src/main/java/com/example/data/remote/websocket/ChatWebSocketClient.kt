package com.example.data.remote.websocket

import android.util.Log
import com.example.core.preferences.UserPreferencesManager
import com.example.core.security.SecureSessionManager
import com.example.data.remote.model.IncomingFriendRequestDto
import com.example.data.remote.model.MessageDto
import com.example.data.remote.model.UserDto
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.random.Random

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

data class MessageReceipt(
    val messageId: String,
    val conversationId: String,
    val status: String,
    val idempotencyKey: String? = null
)

class ChatWebSocketClient(
    private val sessionManager: SecureSessionManager,
    private val preferencesManager: UserPreferencesManager,
    private val moshi: Moshi
) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private var shouldReconnect = true

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<MessageDto>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<MessageDto> = _incomingMessages.asSharedFlow()

    private val _messageReceipts = MutableSharedFlow<MessageReceipt>(extraBufferCapacity = 64)
    val messageReceipts: SharedFlow<MessageReceipt> = _messageReceipts.asSharedFlow()

    private val _incomingFriendRequests = MutableSharedFlow<IncomingFriendRequestDto>(extraBufferCapacity = 32)
    val incomingFriendRequests: SharedFlow<IncomingFriendRequestDto> = _incomingFriendRequests.asSharedFlow()

    private val _friendRequestAccepted = MutableSharedFlow<UserDto>(extraBufferCapacity = 32)
    val friendRequestAccepted: SharedFlow<UserDto> = _friendRequestAccepted.asSharedFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for WS
        .build()

    private val messageAdapter = moshi.adapter(MessageDto::class.java)
    private val friendRequestAdapter = moshi.adapter(IncomingFriendRequestDto::class.java)
    private val userAdapter = moshi.adapter(UserDto::class.java)

    companion object {
        private const val TAG = "ZxChatWebSocket"
        private const val BASE_BACKOFF_MS = 1000L
        private const val MAX_BACKOFF_MS = 30000L
    }

    fun connect() {
        val token = sessionManager.getAccessToken()
        if (token.isNullOrBlank()) {
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        shouldReconnect = true
        reconnectJob?.cancel()
        _connectionState.value = ConnectionState.CONNECTING

        var serverUrl = preferencesManager.getServerUrl().trim()
        if (serverUrl.endsWith("/")) serverUrl = serverUrl.dropLast(1)
        val wsUrl = when {
            serverUrl.startsWith("https://") -> serverUrl.replaceFirst("https://", "wss://") + "/ws?token=$token"
            serverUrl.startsWith("http://") -> serverUrl.replaceFirst("http://", "ws://") + "/ws?token=$token"
            else -> "ws://$serverUrl/ws?token=$token"
        }

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket?.close(1000, "Reconnecting")
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully")
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempt = 0
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
                if (shouldReconnect) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure: ${t.message}")
                _connectionState.value = ConnectionState.DISCONNECTED
                if (shouldReconnect) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")
            val payload = json.optJSONObject("payload") ?: return

            when (type) {
                "MESSAGE_RECEIVED" -> {
                    val message = messageAdapter.fromJson(payload.toString())
                    if (message != null) {
                        scope.launch { _incomingMessages.emit(message) }
                    }
                }
                "MESSAGE_ACK", "MESSAGE_SENT" -> {
                    val messageId = payload.optString("id")
                    val convId = payload.optString("conversationId")
                    val idempotencyKey = payload.optString("idempotencyKey")
                    val status = payload.optString("status", "SENT")
                    val receipt = MessageReceipt(
                        messageId = messageId,
                        conversationId = convId,
                        status = status,
                        idempotencyKey = idempotencyKey.ifBlank { null }
                    )
                    scope.launch { _messageReceipts.emit(receipt) }
                }
                "MESSAGE_READ" -> {
                    val messageId = payload.optString("id")
                    val convId = payload.optString("conversationId")
                    val receipt = MessageReceipt(
                        messageId = messageId,
                        conversationId = convId,
                        status = "READ"
                    )
                    scope.launch { _messageReceipts.emit(receipt) }
                }
                "FRIEND_REQUEST_RECEIVED" -> {
                    val req = friendRequestAdapter.fromJson(payload.toString())
                    if (req != null) {
                        scope.launch { _incomingFriendRequests.emit(req) }
                    }
                }
                "FRIEND_REQUEST_ACCEPTED" -> {
                    val userObj = payload.optJSONObject("user")
                    if (userObj != null) {
                        val user = userAdapter.fromJson(userObj.toString())
                        if (user != null) {
                            scope.launch { _friendRequestAccepted.emit(user) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing incoming WS message: ${e.message}")
        }
    }

    fun sendMessage(conversationId: String, content: String, idempotencyKey: String): Boolean {
        val ws = webSocket ?: return false
        if (_connectionState.value != ConnectionState.CONNECTED) return false

        return try {
            val obj = JSONObject().apply {
                put("type", "SEND_MESSAGE")
                put("payload", JSONObject().apply {
                    put("conversationId", conversationId)
                    put("content", content)
                    put("idempotencyKey", idempotencyKey)
                })
            }
            ws.send(obj.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending WS message: ${e.message}")
            false
        }
    }

    fun sendReadReceipt(conversationId: String): Boolean {
        val ws = webSocket ?: return false
        if (_connectionState.value != ConnectionState.CONNECTED) return false

        return try {
            val obj = JSONObject().apply {
                put("type", "READ_CONVERSATION")
                put("payload", JSONObject().apply {
                    put("conversationId", conversationId)
                })
            }
            ws.send(obj.toString())
        } catch (e: Exception) {
            false
        }
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect || !sessionManager.isLoggedIn()) return

        _connectionState.value = ConnectionState.RECONNECTING
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempt++
            val exponential = min(BASE_BACKOFF_MS * (1L shl min(reconnectAttempt, 5)), MAX_BACKOFF_MS)
            val jitter = Random.nextLong(0, (exponential * 0.2).toLong() + 1)
            val delayMs = exponential + jitter
            Log.d(TAG, "Reconnecting attempt #$reconnectAttempt in ${delayMs}ms...")
            delay(delayMs)
            connect()
        }
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
