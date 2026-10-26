package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.MessageEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    val conversationId: String,
    val peerTitle: String,
    private val repository: ChatRepository
) : ViewModel() {

    val messages: StateFlow<List<MessageEntity>> = repository.observeMessages(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _hasMoreEarlier = MutableStateFlow(true)
    val hasMoreEarlier: StateFlow<Boolean> = _hasMoreEarlier.asStateFlow()

    private val _isLoadingEarlier = MutableStateFlow(false)
    val isLoadingEarlier: StateFlow<Boolean> = _isLoadingEarlier.asStateFlow()

    init {
        loadInitialMessages()
        markAsRead()
    }

    fun onInputTextChange(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        _inputText.value = ""
        viewModelScope.launch {
            _isSending.value = true
            repository.sendMessage(conversationId, text)
            _isSending.value = false
        }
    }

    fun retryMessage(content: String) {
        viewModelScope.launch {
            repository.sendMessage(conversationId, content)
        }
    }

    private fun loadInitialMessages() {
        viewModelScope.launch {
            val result = repository.loadMessages(conversationId)
            result.onSuccess { hasMore ->
                _hasMoreEarlier.value = hasMore
            }
        }
    }

    fun loadEarlierMessages() {
        if (_isLoadingEarlier.value || !_hasMoreEarlier.value) return
        val current = messages.value
        val oldest = current.firstOrNull()?.timestamp ?: return

        viewModelScope.launch {
            _isLoadingEarlier.value = true
            val result = repository.loadMessages(conversationId, cursor = oldest.toString())
            result.onSuccess { hasMore ->
                _hasMoreEarlier.value = hasMore
            }
            _isLoadingEarlier.value = false
        }
    }

    fun markAsRead() {
        viewModelScope.launch {
            repository.markConversationRead(conversationId)
        }
    }
}
