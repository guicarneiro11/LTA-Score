package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.usecases.GetUserVoteHistoryUseCase
import com.guicarneirodev.ltascore.domain.usecases.ShareVoteToTeamFeedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.guicarneirodev.ltascore.android.R
import com.guicarneirodev.ltascore.android.util.StringResources

data class VoteHistoryUiState(
    val isLoading: Boolean = false,
    val isSharing: Boolean = false,
    val history: List<UserVoteHistoryItem> = emptyList(),
    val groupedHistory: Map<String, List<UserVoteHistoryItem>> = emptyMap(),
    val shareSuccess: String? = null,
    val error: String? = null
)

class VoteHistoryViewModel(
    private val getUserVoteHistoryUseCase: GetUserVoteHistoryUseCase,
    private val shareVoteToTeamFeedUseCase: ShareVoteToTeamFeedUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoteHistoryUiState(isLoading = true))
    val uiState: StateFlow<VoteHistoryUiState> = _uiState.asStateFlow()

    init {
        loadVoteHistory()
    }

    fun loadVoteHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, shareSuccess = null)

            try {
                getUserVoteHistoryUseCase().collect { historyItems ->
                    val groupedItems = historyItems.groupBy {
                        "${it.matchId}|${formatDateForGrouping(it.matchDate)}"
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        history = historyItems,
                        groupedHistory = groupedItems
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.history_load_error, e.message ?: "")
                )
            }
        }
    }

    fun shareVoteToTeamFeed(matchId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSharing = true, error = null, shareSuccess = null)

            try {
                val result = shareVoteToTeamFeedUseCase(matchId)

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isSharing = false,
                            shareSuccess = StringResources.getString(R.string.votes_shared)
                        )

                        launch {
                            kotlinx.coroutines.delay(3000)
                            _uiState.value = _uiState.value.copy(shareSuccess = null)
                        }
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isSharing = false,
                            error = StringResources.getStringFormatted(R.string.share_error, e.message ?: "")
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSharing = false,
                    error = StringResources.getStringFormatted(R.string.votes_share_error, e.message ?: "")
                )
            }
        }
    }

    private fun formatDateForGrouping(date: Instant): String {
        val localDate = date.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${localDate.dayOfMonth}/${localDate.monthNumber}/${localDate.year}"
    }
}