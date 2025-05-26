package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.android.util.StringResources
import com.guicarneirodev.ltascore.domain.models.FriendRequest
import com.guicarneirodev.ltascore.domain.models.Friendship
import com.guicarneirodev.ltascore.domain.usecases.ManageFriendshipsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.guicarneirodev.ltascore.android.R

data class FriendsManagementUiState(
    val isLoading: Boolean = false,
    val friendUsername: String = "",
    val friends: List<Friendship> = emptyList(),
    val error: String? = null,
    val success: String? = null
)

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

    fun sendFriendRequest() {
        val username = _uiState.value.friendUsername.trim()

        if (username.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = StringResources.getString(R.string.username_empty)
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
                            friendUsername = "",
                            success = StringResources.getStringFormatted(R.string.friend_request_sent, username)
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: StringResources.getString(R.string.request_send_error)
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.request_send_error, e.message ?: "")
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
                            success = StringResources.getStringFormatted(R.string.friend_removed, friendship.friendUsername)
                        )
                        loadFriends()
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = StringResources.getStringFormatted(R.string.friend_remove_error, e.message ?: "")
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.friend_remove_error, e.message ?: "")
                )
            }
        }
    }

    private fun loadFriends() {
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
                    error = StringResources.getStringFormatted(R.string.friends_load_error, e.message ?: "")
                )
            }
        }
    }

    private fun loadFriendRequests() {
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
                    error = StringResources.getStringFormatted(R.string.friends_load_error, e.message ?: "")
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
                            success = StringResources.getString(R.string.request_accepted)
                        )
                        loadFriends()
                        loadFriendRequests()
                    },
                    onFailure = { e ->
                        _requestsUiState.value = _requestsUiState.value.copy(
                            isLoading = false,
                            error = StringResources.getStringFormatted(R.string.request_accept_error, e.message ?: "")
                        )
                    }
                )
            } catch (e: Exception) {
                _requestsUiState.value = _requestsUiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.request_accept_error, e.message ?: "")
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
                            success = StringResources.getString(R.string.request_declined)
                        )
                        loadFriendRequests()
                    },
                    onFailure = { e ->
                        _requestsUiState.value = _requestsUiState.value.copy(
                            isLoading = false,
                            error = StringResources.getStringFormatted(R.string.request_reject_error, e.message ?: "")
                        )
                    }
                )
            } catch (e: Exception) {
                _requestsUiState.value = _requestsUiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.request_reject_error, e.message ?: "")
                )
            }
        }
    }
}