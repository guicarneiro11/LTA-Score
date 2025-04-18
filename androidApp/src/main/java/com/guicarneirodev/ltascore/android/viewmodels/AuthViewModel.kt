package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.ltascore.api.LoLEsportsApi
import com.guicarneirodev.ltascore.domain.models.User
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    // Estado da tela de login
    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    // Estado da tela de registro
    private val _registerUiState = MutableStateFlow(RegisterUiState())
    val registerUiState: StateFlow<RegisterUiState> = _registerUiState.asStateFlow()

    // Estado da tela de reset de senha
    private val _resetPasswordUiState = MutableStateFlow(ResetPasswordUiState())
    val resetPasswordUiState: StateFlow<ResetPasswordUiState> = _resetPasswordUiState.asStateFlow()

    // Estado geral de autenticação do usuário
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Verifica se o usuário está logado
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _usernameAvailabilityState = MutableStateFlow<UsernameCheckState>(UsernameCheckState.Initial)
    val usernameAvailabilityState: StateFlow<UsernameCheckState> = _usernameAvailabilityState.asStateFlow()

    // Enum para estados de verificação de username
    sealed class UsernameCheckState {
        object Initial : UsernameCheckState()
        object Available : UsernameCheckState()
        data class Unavailable(val message: String) : UsernameCheckState()
        data class Error(val message: String) : UsernameCheckState()
        object Checking : UsernameCheckState()
    }

    init {
        // Observa o estado de autenticação
        viewModelScope.launch {
            userRepository.isUserLoggedIn().collect { isLoggedIn ->
                _isLoggedIn.value = isLoggedIn

                // Atualiza os estados de UI
                _loginUiState.value = _loginUiState.value.copy(isLoggedIn = isLoggedIn)
                _registerUiState.value = _registerUiState.value.copy(isLoggedIn = isLoggedIn)
            }
        }

        // Observa o usuário atual
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                _currentUser.value = user
            }
        }

        // Carregar o logo se a API estiver disponível
        loadLtaCrossLogo()
    }

    private fun loadLtaCrossLogo() {
        // Apenas carregar se a API for fornecida
        loLEsportsApi?.let { api ->
            viewModelScope.launch {
                try {
                    val response = api.getLeagues()
                    val crossLeague = response.data?.leagues?.find { it.slug == "lta_cross" }
                    crossLeague?.let { league ->
                        // Substituir http por https para evitar erros de segurança
                        val secureImageUrl = league.image.replace("http://", "https://")
                        _loginUiState.value = _loginUiState.value.copy(ltaCrossLogo = secureImageUrl)
                    }
                } catch (e: Exception) {
                    println("Erro ao carregar logo LTA Cross: ${e.message}")
                    // Não exibimos erro para não afetar a experiência principal
                }
            }
        }
    }

    fun checkUsernameAvailability(username: String) {
        viewModelScope.launch {
            _usernameAvailabilityState.value = UsernameCheckState.Checking

            try {
                val lowercaseUsername = username.lowercase()

                // Validações locais primeiro
                if (username.length < 3 || username.length > 20) {
                    _usernameAvailabilityState.value = UsernameCheckState.Unavailable(
                        "Nome de usuário deve ter entre 3 e 20 caracteres"
                    )
                    return@launch
                }

                val usernameRegex = Regex("^[a-zA-Z0-9_]+$")
                if (!usernameRegex.matches(username)) {
                    _usernameAvailabilityState.value = UsernameCheckState.Unavailable(
                        "Use apenas letras, números e _"
                    )
                    return@launch
                }

                // Verificação direta no Firestore
                val snapshot = firestore
                    .collection("usernames")
                    .document(lowercaseUsername)
                    .get()
                    .await()

                _usernameAvailabilityState.value = if (snapshot.exists()) {
                    UsernameCheckState.Unavailable("Nome de usuário já está em uso")
                } else {
                    UsernameCheckState.Available
                }

            } catch (e: Exception) {
                _usernameAvailabilityState.value = UsernameCheckState.Error(
                    "Erro ao verificar nome de usuário: ${e.message}"
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
                    onSuccess = {
                        _loginUiState.value = _loginUiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true
                        )
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
                    error = "Erro ao fazer login: ${e.message}"
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
                    error = "Erro ao criar conta: ${e.message}"
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
                    error = "Erro ao enviar email: ${e.message}"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userRepository.signOut()
        }
    }

    // Mapeia erros do Firebase Auth para mensagens amigáveis
    private fun mapFirebaseAuthError(e: Throwable): String {
        return when {
            e.message?.contains("Nome de usuário já está em uso") == true -> "Nome de usuário já está em uso"
            e.message?.contains("badly formatted") == true -> "Email inválido"
            e.message?.contains("password is invalid") == true -> "Senha incorreta"
            e.message?.contains("no user record") == true -> "Usuário não encontrado"
            e.message?.contains("email already in use") == true -> "Email já cadastrado"
            e.message?.contains("network error") == true -> "Erro de conexão"
            e.message?.contains("weak password") == true -> "Senha muito fraca"
            else -> "Erro de autenticação: ${e.message}"
        }
    }
}