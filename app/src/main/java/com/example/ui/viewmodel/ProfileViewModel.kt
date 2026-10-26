package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.model.UserDto
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<UserDto?>(null)
    val profile: StateFlow<UserDto?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getMyProfile()
            result.onSuccess { _profile.value = it }
            _isLoading.value = false
        }
    }

    fun startEditing() {
        _isEditing.value = true
    }

    fun cancelEditing() {
        _isEditing.value = false
    }

    fun saveProfile(displayName: String, bio: String, phoneNumber: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateMyProfile(displayName.trim(), bio.trim(), phoneNumber?.trim())
            result.fold(
                onSuccess = {
                    _feedback.value = "Profile saved successfully"
                    _isEditing.value = false
                    loadProfile()
                },
                onFailure = { err ->
                    _feedback.value = err.message ?: "Failed to update profile"
                }
            )
            _isLoading.value = false
        }
    }

    fun clearFeedback() {
        _feedback.value = null
    }
}
