package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.domain.models.Friendship
import com.guicarneirodev.ltascore.domain.usecases.ManageFriendshipsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciamento de amigos no perfil do usuário
 */
data class FriendsManagementUiState(
    val isLoading: Boolean = false,
    val friendUsername: String = "",
    val friends: List<Friendship> = emptyList(),
    val error: String? = null,
    val success: String? = null
)

class FriendsViewModel(
    private val manageFriendshipsUseCase: ManageFriendshipsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsManagementUiState())
    val uiState: StateFlow<FriendsManagementUiState> = _uiState.asStateFlow()

    init {
        loadFriends()
    }

    fun updateFriendUsername(username: String) {
        _uiState.value = _uiState.value.copy(
            friendUsername = username,
            error = null,
            success = null
        )
    }

    fun addFriend() {
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
                val result = manageFriendshipsUseCase.addFriend(username)

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            friendUsername = "", // Limpa o campo
                            success = "$username adicionado(a) como amigo(a)!"
                        )
                        // Recarregar a lista de amigos
                        loadFriends()
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Erro ao adicionar amigo"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao adicionar amigo: ${e.message}"
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
}