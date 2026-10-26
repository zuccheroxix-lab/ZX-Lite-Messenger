package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.FriendEntity
import com.example.data.local.entity.FriendRequestEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.data.local.entity.ConversationEntity

class FriendsViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    val friends: StateFlow<List<FriendEntity>> = repository.observeFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingRequests: StateFlow<List<FriendRequestEntity>> = repository.observeIncomingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outgoingRequests: StateFlow<List<FriendRequestEntity>> = repository.observeOutgoingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _createdConversation = MutableStateFlow<ConversationEntity?>(null)
    val createdConversation: StateFlow<ConversationEntity?> = _createdConversation.asStateFlow()

    init {
        refresh()
    }

    fun startChatWithFriend(username: String) {
        viewModelScope.launch {
            val result = repository.createDirectConversation(username)
            result.fold(
                onSuccess = { conv -> _createdConversation.value = conv },
                onFailure = { err -> _errorMessage.value = err.message ?: "Failed to open chat" }
            )
        }
    }

    fun onConversationNavigated() {
        _createdConversation.value = null
    }


    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshFriends()
            repository.refreshRequests()
            _isRefreshing.value = false
        }
    }

    fun sendRequest(username: String?, phoneNumber: String?) {
        viewModelScope.launch {
            val result = repository.sendFriendRequest(username, phoneNumber)
            result.fold(
                onSuccess = { msg ->
                    _actionMessage.value = msg
                    refresh()
                },
                onFailure = { err ->
                    _errorMessage.value = err.message ?: "Failed to send request"
                }
            )
        }
    }

    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            val result = repository.acceptFriendRequest(requestId)
            result.fold(
                onSuccess = {
                    _actionMessage.value = "Friend request accepted"
                    refresh()
                },
                onFailure = { err ->
                    _errorMessage.value = err.message ?: "Failed to accept"
                }
            )
        }
    }

    fun declineRequest(requestId: String) {
        viewModelScope.launch {
            val result = repository.declineFriendRequest(requestId)
            result.fold(
                onSuccess = {
                    _actionMessage.value = "Request declined"
                    refresh()
                },
                onFailure = { err ->
                    _errorMessage.value = err.message ?: "Failed to decline"
                }
            )
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            val result = repository.cancelFriendRequest(requestId)
            result.fold(
                onSuccess = {
                    _actionMessage.value = "Request cancelled"
                    refresh()
                },
                onFailure = { err ->
                    _errorMessage.value = err.message ?: "Failed to cancel"
                }
            )
        }
    }

    fun unfriend(userId: String) {
        viewModelScope.launch {
            repository.unfriend(userId)
            refresh()
        }
    }

    fun clearFeedback() {
        _actionMessage.value = null
        _errorMessage.value = null
    }
}
