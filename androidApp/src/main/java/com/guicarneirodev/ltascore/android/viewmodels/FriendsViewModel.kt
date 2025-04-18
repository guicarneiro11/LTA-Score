package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.domain.models.FriendRequest
import com.guicarneirodev.ltascore.domain.models.Friendship
import com.guicarneirodev.ltascore.domain.usecases.ManageFriendshipsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Estado existente
data class FriendsManagementUiState(
    val isLoading: Boolean = false,
    val friendUsername: String = "",
    val friends: List<Friendship> = emptyList(),
    val error: String? = null,
    val success: String? = null
)

// Novo estado para solicitações
data class FriendRequestsUiState(
    val isLoading: Boolean = false,
    val requests: List<FriendRequest> = emptyList(),
    val error: String? = null,
    val success: String? = null
)

class FriendsViewModel(
    private val manageFriendshipsUseCase: ManageFriendshipsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsManagementUiState())
    val uiState: StateFlow<FriendsManagementUiState> = _uiState.asStateFlow()

    // Novo estado para solicitações
    private val _requestsUiState = MutableStateFlow(FriendRequestsUiState())
    val requestsUiState: StateFlow<FriendRequestsUiState> = _requestsUiState.asStateFlow()

    init {
        loadFriends()
        loadFriendRequests()
    }

    fun updateFriendUsername(username: String) {
        _uiState.value = _uiState.value.copy(
            friendUsername = username,
            error = null,
            success = null
        )
    }

    // Modificar método existente para enviar solicitação em vez de adicionar diretamente
    fun addFriend() {
        sendFriendRequest()
    }

    // Novo método para enviar solicitações
    fun sendFriendRequest() {
        val username = _uiState.value.friendUsername.trim()

        if (username.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "Nome de usuário não pode ser vazio"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )

            try {
                val result = manageFriendshipsUseCase.sendFriendRequest(username)

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            friendUsername = "", // Limpa o campo
                            success = "Solicitação enviada para $username"
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Erro ao enviar solicitação"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao enviar solicitação: ${e.message}"
                )
            }
        }
    }

    fun removeFriend(friendship: Friendship) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )

            try {
                val result = manageFriendshipsUseCase.removeFriend(friendship.friendId)

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            success = "${friendship.friendUsername} removido(a) da sua lista de amigos"
                        )
                        // Recarregar a lista de amigos
                        loadFriends()
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Erro ao remover amigo"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao remover amigo: ${e.message}"
                )
            }
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                manageFriendshipsUseCase.getUserFriends().collect { friends ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        friends = friends
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar amigos: ${e.message}"
                )
            }
        }
    }

    // Novos métodos para gerenciar solicitações

    fun loadFriendRequests() {
        viewModelScope.launch {
            _requestsUiState.value = _requestsUiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                manageFriendshipsUseCase.getPendingFriendRequests().collect { requests ->
                    _requestsUiState.value = _requestsUiState.value.copy(
                        isLoading = false,
                        requests = requests
                    )
                }
            } catch (e: Exception) {
                _requestsUiState.value = _requestsUiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar solicitações: ${e.message}"
                )
            }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            _requestsUiState.value = _requestsUiState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )

            try {
                val result = manageFriendshipsUseCase.acceptFriendRequest(requestId)

                result.fold(
                    onSuccess = {
                        _requestsUiState.value = _requestsUiState.value.copy(
                            isLoading = false,
                            success = "Solicitação aceita"
                        )
                        // Recarregar amigos e solicitações
                        loadFriends()
                        loadFriendRequests()
                    },
                    onFailure = { e ->
                        _requestsUiState.value = _requestsUiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Erro ao aceitar solicitação"
                        )
                    }
                )
            } catch (e: Exception) {
                _requestsUiState.value = _requestsUiState.value.copy(
                    isLoading = false,
                    error = "Erro ao aceitar solicitação: ${e.message}"
                )
            }
        }
    }

    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            _requestsUiState.value = _requestsUiState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )

            try {
                val result = manageFriendshipsUseCase.rejectFriendRequest(requestId)

                result.fold(
                    onSuccess = {
                        _requestsUiState.value = _requestsUiState.value.copy(
                            isLoading = false,
                            success = "Solicitação recusada"
                        )
                        // Recarregar solicitações
                        loadFriendRequests()
                    },
                    onFailure = { e ->
                        _requestsUiState.value = _requestsUiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Erro ao recusar solicitação"
                        )
                    }
                )
            } catch (e: Exception) {
                _requestsUiState.value = _requestsUiState.value.copy(
                    isLoading = false,
                    error = "Erro ao recusar solicitação: ${e.message}"
                )
            }
        }
    }
}