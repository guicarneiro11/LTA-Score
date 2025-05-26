package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.ltascore.android.data.cache.UserEvents
import com.guicarneirodev.ltascore.android.util.StringResources
import com.guicarneirodev.ltascore.api.LoLEsportsApi
import com.guicarneirodev.ltascore.domain.models.User
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.guicarneirodev.ltascore.android.R
import com.guicarneirodev.ltascore.android.data.repository.NotificationTokenRepository

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val ltaCrossLogo: String? = null
)

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)

data class ResetPasswordUiState(
    val isLoading: Boolean = false,
    val isEmailSent: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val userRepository: UserRepository,
    private val loLEsportsApi: LoLEsportsApi? = null,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _registerUiState = MutableStateFlow(RegisterUiState())
    val registerUiState: StateFlow<RegisterUiState> = _registerUiState.asStateFlow()

    private val _resetPasswordUiState = MutableStateFlow(ResetPasswordUiState())
    val resetPasswordUiState: StateFlow<ResetPasswordUiState> = _resetPasswordUiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _usernameAvailabilityState = MutableStateFlow<UsernameCheckState>(UsernameCheckState.Initial)
    val usernameAvailabilityState: StateFlow<UsernameCheckState> = _usernameAvailabilityState.asStateFlow()

    sealed class UsernameCheckState {
        data object Initial : UsernameCheckState()
        data object Available : UsernameCheckState()
        data class Unavailable(val message: String) : UsernameCheckState()
        data class Error(val message: String) : UsernameCheckState()
        data object Checking : UsernameCheckState()
    }

    private val _forceUserRefresh = MutableStateFlow(0)

    fun triggerUserRefresh() {
        _forceUserRefresh.value += 1
    }

    fun refreshCurrentUser() {
        viewModelScope.launch {
            userRepository.refreshCurrentUser()
            _forceUserRefresh.value += 1
            println("AuthViewModel forced refresh triggered")
        }
    }

    init {
        viewModelScope.launch {
            UserEvents.userUpdated.collect {
                userRepository.refreshCurrentUser()
                triggerUserRefresh()
                println("AuthViewModel recebeu evento de atualização e forçou refresh")
            }
        }

        viewModelScope.launch {
            userRepository.isUserLoggedIn().collect { isLoggedIn ->
                _isLoggedIn.value = isLoggedIn

                _loginUiState.value = _loginUiState.value.copy(isLoggedIn = isLoggedIn)
                _registerUiState.value = _registerUiState.value.copy(isLoggedIn = isLoggedIn)
            }
        }

        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                _currentUser.value = user
            }
        }

        loadLtaCrossLogo()
    }

    private fun loadLtaCrossLogo() {
        loLEsportsApi?.let { api ->
            viewModelScope.launch {
                try {
                    val response = api.getLeagues()
                    val crossLeague = response.data?.leagues?.find { it.slug == "lta_cross" }
                    crossLeague?.let { league ->
                        val secureImageUrl = league.image.replace("http://", "https://")
                        _loginUiState.value = _loginUiState.value.copy(ltaCrossLogo = secureImageUrl)
                    }
                } catch (e: Exception) {
                    println("Erro ao carregar logo LTA Cross: ${e.message}")
                }
            }
        }
    }

    fun checkUsernameAvailability(username: String) {
        viewModelScope.launch {
            _usernameAvailabilityState.value = UsernameCheckState.Checking

            try {
                val lowercaseUsername = username.lowercase()

                if (username.length < 3 || username.length > 20) {
                    _usernameAvailabilityState.value = UsernameCheckState.Unavailable(
                        StringResources.getString(R.string.username_required_length)
                    )
                    return@launch
                }

                val usernameRegex = Regex("^[a-zA-Z0-9_]+$")
                if (!usernameRegex.matches(username)) {
                    _usernameAvailabilityState.value = UsernameCheckState.Unavailable(
                        StringResources.getString(R.string.username_format)
                    )
                    return@launch
                }

                val snapshot = firestore
                    .collection("usernames")
                    .document(lowercaseUsername)
                    .get()
                    .await()

                _usernameAvailabilityState.value = if (snapshot.exists()) {
                    UsernameCheckState.Unavailable(StringResources.getString(R.string.username_in_use))
                } else {
                    UsernameCheckState.Available
                }

            } catch (e: Exception) {
                _usernameAvailabilityState.value = UsernameCheckState.Error(
                    StringResources.getStringFormatted(R.string.username_check_error, e.message ?: "")
                )
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginUiState.value = _loginUiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val result = userRepository.signIn(email, password)
                result.fold(
                    onSuccess = { user ->
                        _loginUiState.value = _loginUiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true
                        )
                        registerForNotifications(user.id)
                    },
                    onFailure = { e ->
                        _loginUiState.value = _loginUiState.value.copy(
                            isLoading = false,
                            error = mapFirebaseAuthError(e)
                        )
                    }
                )
            } catch (e: Exception) {
                _loginUiState.value = _loginUiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.login_error, e.message ?: "")
                )
            }
        }
    }

    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            _registerUiState.value = _registerUiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val result = userRepository.signUp(email, password, username)
                result.fold(
                    onSuccess = {
                        _registerUiState.value = _registerUiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true
                        )
                    },
                    onFailure = { e ->
                        _registerUiState.value = _registerUiState.value.copy(
                            isLoading = false,
                            error = mapFirebaseAuthError(e)
                        )
                    }
                )
            } catch (e: Exception) {
                _registerUiState.value = _registerUiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.register_error, e.message ?: "")
                )
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _resetPasswordUiState.value = _resetPasswordUiState.value.copy(
                isLoading = true,
                isEmailSent = false,
                error = null
            )

            try {
                val result = userRepository.resetPassword(email)
                result.fold(
                    onSuccess = {
                        _resetPasswordUiState.value = _resetPasswordUiState.value.copy(
                            isLoading = false,
                            isEmailSent = true
                        )
                    },
                    onFailure = { e ->
                        _resetPasswordUiState.value = _resetPasswordUiState.value.copy(
                            isLoading = false,
                            error = mapFirebaseAuthError(e)
                        )
                    }
                )
            } catch (e: Exception) {
                _resetPasswordUiState.value = _resetPasswordUiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.email_send_error, e.message ?: "")
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userRepository.signOut()
        }
    }

    private fun mapFirebaseAuthError(e: Throwable): String {
        return when {
            e.message?.contains("Username already in use") == true ->
                StringResources.getString(R.string.username_in_use)
            e.message?.contains("Badly formatted") == true ->
                StringResources.getString(R.string.invalid_email)
            e.message?.contains("Password is invalid") == true ->
                StringResources.getString(R.string.wrong_password)
            e.message?.contains("No user record") == true ->
                StringResources.getString(R.string.user_not_found)
            e.message?.contains("Email already in use") == true ->
                StringResources.getString(R.string.email_in_use)
            e.message?.contains("Network error") == true ->
                StringResources.getString(R.string.connection_error)
            e.message?.contains("Weak password") == true ->
                StringResources.getString(R.string.weak_password)
            else -> StringResources.getStringFormatted(R.string.auth_error, e.message ?: "")
        }
    }

    private fun registerForNotifications(userId: String) {
        viewModelScope.launch {
            try {
                val tokenRepository = NotificationTokenRepository()
                tokenRepository.registerUserForNotifications(userId)
            } catch (e: Exception) {
                println("Failed to register for notifications: ${e.message}")
            }
        }
    }
}