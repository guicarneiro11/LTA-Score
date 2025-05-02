package com.guicarneirodev.ltascore.android.viewmodels

class NotificationSettingsViewModel(
    private val tokenRepository: NotificationTokenRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    data class NotificationSettingsUiState(
        val isLoading: Boolean = false,
        val matchNotifications: Boolean = true,
        val liveMatchNotifications: Boolean = true,
        val resultNotifications: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
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
                            matchNotifications = prefsDoc.getBoolean("matchNotifications") ?: true,
                            liveMatchNotifications = prefsDoc.getBoolean("liveMatchNotifications") ?: true,
                            resultNotifications = prefsDoc.getBoolean("resultNotifications") ?: true,
                            isLoading = false
                        )
                    } else {
                        // Register with default values if no preferences exist
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