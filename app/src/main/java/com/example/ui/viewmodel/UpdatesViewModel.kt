package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.UserStatusEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UpdatesViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    val statuses: StateFlow<List<UserStatusEntity>> = repository.observeStatuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _postSuccess = MutableStateFlow(false)
    val postSuccess: StateFlow<Boolean> = _postSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshStatuses()
            _isRefreshing.value = false
        }
    }

    fun postStatus(text: String) {
        if (text.trim().isEmpty()) return

        viewModelScope.launch {
            _errorMessage.value = null
            val result = repository.postStatus(text)
            result.fold(
                onSuccess = {
                    _postSuccess.value = true
                    refresh()
                },
                onFailure = { err ->
                    _errorMessage.value = err.message ?: "Failed to post status"
                }
            )
        }
    }

    fun clearFeedback() {
        _postSuccess.value = false
        _errorMessage.value = null
    }

    fun getMyUsername(): String = repository.sessionManager.getUsername() ?: ""
    fun getMyDisplayName(): String = repository.sessionManager.getDisplayName() ?: ""
}
