package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ConversationEntity
import com.example.data.remote.model.UserDto
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserSearchViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserDto>>(emptyList())
    val searchResults: StateFlow<List<UserDto>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _createdConversation = MutableStateFlow<ConversationEntity?>(null)
    val createdConversation: StateFlow<ConversationEntity?> = _createdConversation.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.trim().isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _isSearching.value = true
            _errorMessage.value = null
            val result = repository.searchUsers(query)
            result.fold(
                onSuccess = { users ->
                    val myUsername = repository.sessionManager.getUsername()
                    _searchResults.value = users.filter { it.username != myUsername }
                },
                onFailure = {
                    _searchResults.value = emptyList()
                    _errorMessage.value = "Failed to search users"
                }
            )
            _isSearching.value = false
        }
    }

    fun startChatWith(peerUsername: String) {
        viewModelScope.launch {
            _isSearching.value = true
            val result = repository.createDirectConversation(peerUsername)
            result.fold(
                onSuccess = { conv ->
                    _createdConversation.value = conv
                },
                onFailure = { err ->
                    _errorMessage.value = err.message ?: "Failed to open conversation"
                }
            )
            _isSearching.value = false
        }
    }

    fun onConversationNavigated() {
        _createdConversation.value = null
    }
}
