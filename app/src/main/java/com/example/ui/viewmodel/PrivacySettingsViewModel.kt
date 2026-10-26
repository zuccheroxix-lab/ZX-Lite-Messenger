package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.model.PrivacySettingsDto
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrivacySettingsViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(PrivacySettingsDto())
    val settings: StateFlow<PrivacySettingsDto> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getPrivacySettings()
            result.onSuccess { _settings.value = it }
            _isLoading.value = false
        }
    }

    fun toggleAutoAccept(enabled: Boolean) {
        val updated = _settings.value.copy(autoAcceptMatchedContacts = enabled)
        update(updated, if (enabled) "Auto-accept enabled for mutual verified contacts" else "Auto-accept disabled")
    }

    fun togglePhoneDiscovery(enabled: Boolean) {
        val updated = _settings.value.copy(allowPhoneDiscovery = enabled)
        update(updated, if (enabled) "Phone discovery enabled" else "Phone discovery hidden")
    }

    fun toggleReadReceipts(enabled: Boolean) {
        val updated = _settings.value.copy(readReceiptsEnabled = enabled)
        update(updated, if (enabled) "Read receipts enabled" else "Read receipts disabled")
    }

    private fun update(newSettings: PrivacySettingsDto, msg: String) {
        viewModelScope.launch {
            _settings.value = newSettings
            val result = repository.updatePrivacySettings(newSettings)
            result.fold(
                onSuccess = {
                    _settings.value = it
                    _feedback.value = msg
                },
                onFailure = { err ->
                    _feedback.value = err.message ?: "Failed to save settings"
                }
            )
        }
    }

    fun clearFeedback() {
        _feedback.value = null
    }
}
