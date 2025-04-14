package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.usecases.GetUserVoteHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class VoteHistoryUiState(
    val isLoading: Boolean = false,
    val history: List<UserVoteHistoryItem> = emptyList(),
    val groupedHistory: Map<String, List<UserVoteHistoryItem>> = emptyMap(),
    val error: String? = null
)

class VoteHistoryViewModel(
    private val getUserVoteHistoryUseCase: GetUserVoteHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoteHistoryUiState(isLoading = true))
    val uiState: StateFlow<VoteHistoryUiState> = _uiState.asStateFlow()

    init {
        loadVoteHistory()
    }

    fun loadVoteHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                getUserVoteHistoryUseCase().collect { historyItems ->
                    // Agrupar votos por matchId para melhor visualização
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
                    error = "Erro ao carregar histórico: ${e.message}"
                )
            }
        }
    }

    private fun formatDateForGrouping(date: Instant): String {
        // Formatar data para agrupar por dia
        val localDate = date.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${localDate.dayOfMonth}/${localDate.monthNumber}/${localDate.year}"
    }
}