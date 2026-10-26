package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val username: String = "",
    val displayName: String = "",
    val password: String = "",
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isServerSettingsOpen: Boolean = false,
    val serverUrlInput: String = ""
)

class AuthViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            serverUrlInput = repository.preferencesManager.getServerUrl()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, errorMessage = null)
    }

    fun onDisplayNameChange(value: String) {
        _uiState.value = _uiState.value.copy(displayName = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(
            isRegisterMode = !_uiState.value.isRegisterMode,
            errorMessage = null
        )
    }

    fun openServerSettings() {
        _uiState.value = _uiState.value.copy(
            isServerSettingsOpen = true,
            serverUrlInput = repository.preferencesManager.getServerUrl()
        )
    }

    fun closeServerSettings() {
        _uiState.value = _uiState.value.copy(isServerSettingsOpen = false)
    }

    fun onServerUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(serverUrlInput = url)
    }

    fun saveServerUrl() {
        repository.preferencesManager.setServerUrl(_uiState.value.serverUrlInput)
        closeServerSettings()
    }

    fun setDemoMode(enabled: Boolean) {
        repository.preferencesManager.setDemoGatewayEnabled(enabled)
    }

    fun submit() {
        val state = _uiState.value
        val username = state.username.trim()
        val password = state.password

        if (username.length < 3) {
            _uiState.value = state.copy(errorMessage = "Username must be at least 3 characters")
            return
        }
        if (password.length < 6) {
            _uiState.value = state.copy(errorMessage = "Password must be at least 6 characters")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = if (state.isRegisterMode) {
                val displayName = state.displayName.trim().ifEmpty { username }
                repository.register(username, displayName, password)
            } else {
                repository.login(username, password)
            }

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Authentication failed"
                    )
                }
            )
        }
    }
}
