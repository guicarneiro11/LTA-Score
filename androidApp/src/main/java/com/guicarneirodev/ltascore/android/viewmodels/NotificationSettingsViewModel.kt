package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.ltascore.android.R
import com.guicarneirodev.ltascore.android.data.repository.NotificationTokenRepository
import com.guicarneirodev.ltascore.android.util.StringResources
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationSettingsViewModel(
    private val tokenRepository: NotificationTokenRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    data class NotificationSettingsUiState(
        val isLoading: Boolean = false,
        val matchNotifications: Boolean = true,
        val liveMatchNotifications: Boolean = true,
        val resultNotifications: Boolean = true,
        val error: String? = null,
        val success: String? = null
    )

    private val _matchId = MutableStateFlow("114217106692001382")
    val matchId = _matchId.asStateFlow()

    private val _uiState = MutableStateFlow(NotificationSettingsUiState(
        success = "Teste de notificação enviado"
    )

    )
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserPreferences()
    }

    private fun loadUserPreferences() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val user = userRepository.getCurrentUser().first()

                if (user != null) {
                    val prefsDoc = FirebaseFirestore.getInstance()
                        .collection("user_tokens")
                        .document(user.id)
                        .get()
                        .await()

                    if (prefsDoc.exists()) {
                        _uiState.value = _uiState.value.copy(
                            matchNotifications = prefsDoc.getBoolean("matchNotifications") != false,
                            liveMatchNotifications = prefsDoc.getBoolean("liveMatchNotifications") != false,
                            resultNotifications = prefsDoc.getBoolean("resultNotifications") != false,
                            isLoading = false
                        )
                    } else {
                        tokenRepository.registerUserForNotifications(user.id)
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = StringResources.getString(R.string.not_logged_in)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(
                        R.string.load_preferences_error,
                        e.message ?: ""
                    )
                )
            }
        }
    }

    fun updateMatchNotifications(enabled: Boolean) {
        updatePreference { tokenRepository.updateNotificationPreferences(it, matchNotifications = enabled) }
        _uiState.value = _uiState.value.copy(matchNotifications = enabled)
    }

    fun updateLiveMatchNotifications(enabled: Boolean) {
        updatePreference { tokenRepository.updateNotificationPreferences(it, liveMatchNotifications = enabled) }
        _uiState.value = _uiState.value.copy(liveMatchNotifications = enabled)
    }

    fun updateResultNotifications(enabled: Boolean) {
        updatePreference { tokenRepository.updateNotificationPreferences(it, resultNotifications = enabled) }
        _uiState.value = _uiState.value.copy(resultNotifications = enabled)
    }

    private fun updatePreference(update: suspend (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val userId = userRepository.getCurrentUser().first()?.id

                if (userId != null) {
                    update(userId)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = StringResources.getString(R.string.not_logged_in)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

}