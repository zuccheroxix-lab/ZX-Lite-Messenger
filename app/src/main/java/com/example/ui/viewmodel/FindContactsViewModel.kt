package com.example.ui.viewmodel

import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.model.ContactMatchDto
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FindContactsViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _matches = MutableStateFlow<List<ContactMatchDto>>(emptyList())
    val matches: StateFlow<List<ContactMatchDto>> = _matches.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    fun syncContacts(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val phoneNumbers = fetchDeviceContacts(context)
            if (phoneNumbers.isEmpty()) {
                _isLoading.value = false
                _infoMessage.value = "No phone numbers found in device contacts"
                return@launch
            }

            val result = repository.discoverContacts(phoneNumbers)
            result.fold(
                onSuccess = { list ->
                    _matches.value = list
                    if (list.isEmpty()) {
                        _infoMessage.value = "None of your contacts are currently registered on ZX Lite"
                    }
                },
                onFailure = { err ->
                    _errorMessage.value = err.message ?: "Failed to discover contacts"
                }
            )
            _isLoading.value = false
        }
    }

    fun addFriend(username: String) {
        viewModelScope.launch {
            val result = repository.sendFriendRequest(username, null)
            result.fold(
                onSuccess = { msg ->
                    _infoMessage.value = msg
                    // Mark as requested in current list
                    _matches.value = _matches.value.map {
                        if (it.username == username) it.copy(isFriend = true) else it
                    }
                },
                onFailure = { err ->
                    _errorMessage.value = err.message ?: "Failed to send friend request"
                }
            )
        }
    }

    private suspend fun fetchDeviceContacts(context: Context): List<String> = withContext(Dispatchers.IO) {
        val numbers = mutableSetOf<String>()
        try {
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            cursor?.use {
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val raw = it.getString(numberIdx)
                    if (!raw.isNullOrBlank()) {
                        // Normalize phone number (E.164 cleaning)
                        val cleaned = raw.replace("[^0-9+]".toRegex(), "")
                        if (cleaned.length >= 7) {
                            numbers.add(cleaned)
                        }
                    }
                    if (numbers.size >= 100) break // Bound discovery queries to 100 numbers per request
                }
            }
        } catch (_: Exception) {}
        numbers.toList()
    }

    fun clearFeedback() {
        _errorMessage.value = null
        _infoMessage.value = null
    }
}
