package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.VoteSummary
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import com.guicarneirodev.ltascore.domain.usecases.GetMatchByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MatchSummaryUiState(
    val isLoading: Boolean = false,
    val match: Match? = null,
    val voteSummaries: List<VoteSummary> = emptyList(),
    val userHasVoted: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel que gerencia os dados para a tela de resumo da partida
 */
class MatchSummaryViewModel(
    private val getMatchByIdUseCase: GetMatchByIdUseCase,
    private val voteRepository: VoteRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchSummaryUiState(isLoading = true))
    val uiState: StateFlow<MatchSummaryUiState> = _uiState.asStateFlow()

    /**
     * Carrega os dados da partida e seus resumos de votação
     */
    fun loadMatch(matchId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Obtém o usuário atual
                val currentUser = userRepository.getCurrentUser().first()

                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuário não autenticado"
                    )
                    return@launch
                }

                // Verifica se o usuário já votou nesta partida
                val userHasVoted = voteRepository.hasUserVotedForMatch(currentUser.id, matchId).first()

                // Obtém os detalhes da partida
                val match = getMatchByIdUseCase(matchId).first()

                if (match == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Partida não encontrada"
                    )
                    return@launch
                }

                // Obtém os resumos de votação dos jogadores
                val voteSummaries = voteRepository.getMatchVoteSummary(matchId).first()

                // Atualiza o estado da UI
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    match = match,
                    voteSummaries = voteSummaries,
                    userHasVoted = userHasVoted
                )

                // Continua observando as mudanças nos resumos de votação
                observeVoteSummaries(matchId)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar dados: ${e.message}"
                )
            }
        }
    }

    /**
     * Observa as mudanças nos resumos de votação em tempo real
     */
    private fun observeVoteSummaries(matchId: String) {
        viewModelScope.launch {
            voteRepository.getMatchVoteSummary(matchId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = "Erro ao atualizar resumos: ${e.message}"
                    )
                }
                .collect { summaries ->
                    _uiState.value = _uiState.value.copy(
                        voteSummaries = summaries
                    )
                }
        }
    }
}

fun List<VoteSummary>.getTeamAverageRating(teamPlayers: List<String>): Double {
    val teamSummaries = filter { it.playerId in teamPlayers }
    return if (teamSummaries.isNotEmpty()) {
        teamSummaries.sumOf { it.averageRating } / teamSummaries.size
    } else {
        0.0
    }
}