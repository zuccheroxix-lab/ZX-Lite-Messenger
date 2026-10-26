package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BlockedUserEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlockedUsersViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    val blockedUsers: StateFlow<List<BlockedUserEntity>> = repository.observeBlockedUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshBlockedUsers()
            _isRefreshing.value = false
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            val result = repository.unblockUser(userId)
            result.fold(
                onSuccess = {
                    _message.value = "User unblocked"
                    refresh()
                },
                onFailure = { err ->
                    _message.value = err.message ?: "Failed to unblock"
                }
            )
        }
    }

    fun clearFeedback() {
        _message.value = null
    }
}
