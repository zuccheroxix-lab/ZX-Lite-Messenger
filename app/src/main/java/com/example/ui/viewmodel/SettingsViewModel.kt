package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val prefs = repository.preferencesManager

    val serverUrl: StateFlow<String> = prefs.serverUrlFlow
    val fontScale: StateFlow<Float> = prefs.fontScaleFlow
    val darkTheme: StateFlow<Boolean> = prefs.darkThemeFlow
    val notifications: StateFlow<Boolean> = prefs.notificationsFlow

    private val _serverUrlInput = MutableStateFlow(prefs.getServerUrl())
    val serverUrlInput: StateFlow<String> = _serverUrlInput.asStateFlow()

    private val _isServerDialogVisible = MutableStateFlow(false)
    val isServerDialogVisible: StateFlow<Boolean> = _isServerDialogVisible.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showServerDialog() {
        _serverUrlInput.value = prefs.getServerUrl()
        _isServerDialogVisible.value = true
    }

    fun hideServerDialog() {
        _isServerDialogVisible.value = false
    }

    fun onServerUrlInputChange(value: String) {
        _serverUrlInput.value = value
    }

    fun saveServerUrl() {
        prefs.setServerUrl(_serverUrlInput.value)
        _isServerDialogVisible.value = false
        repository.wsClient.connect()
        _toastMessage.value = "Server URL updated"
    }

    fun setFontScale(scale: Float) {
        prefs.setFontScale(scale)
    }

    fun toggleDarkTheme() {
        prefs.setDarkTheme(!darkTheme.value)
    }

    fun toggleNotifications() {
        prefs.setNotificationsEnabled(!notifications.value)
    }

    fun clearLocalCache() {
        viewModelScope.launch {
            repository.clearLocalCache()
            _toastMessage.value = "Local message cache cleared"
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onLoggedOut()
        }
    }

    fun getUsername(): String = repository.sessionManager.getUsername() ?: ""
    fun getDisplayName(): String = repository.sessionManager.getDisplayName() ?: ""
    fun getUserId(): String = repository.sessionManager.getUserId() ?: ""
}
