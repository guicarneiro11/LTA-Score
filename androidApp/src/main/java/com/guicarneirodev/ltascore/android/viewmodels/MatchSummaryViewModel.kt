package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.android.data.repository.UserPreferencesRepository
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.VoteSummary
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import com.guicarneirodev.ltascore.domain.usecases.GetMatchByIdUseCase
import kotlinx.coroutines.delay
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
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository
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
                // Adiciona um pequeno atraso para permitir que os dados sejam atualizados no Firestore
                delay(500)

                // Obtém o usuário atual
                val currentUser = userRepository.getCurrentUser().first()

                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuário não autenticado"
                    )
                    return@launch
                }

                // Tentamos buscar os detalhes da partida com até 3 tentativas
                var match: Match? = null
                var attempts = 0
                while (match == null && attempts < 3) {
                    match = getMatchByIdUseCase(matchId).first()
                    if (match == null) {
                        delay(500) // Espera meio segundo antes de tentar novamente
                        attempts++
                    }
                }

                if (match == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Partida não encontrada após várias tentativas"
                    )
                    return@launch
                }

                // Primeiro verifica no DataStore local se o usuário já votou
                val hasVotedLocally = userPreferencesRepository.hasUserVotedForMatch(currentUser.id, matchId).first()

                // Se não temos registro local, verificamos no Firestore
                var userHasVoted = hasVotedLocally
                if (!hasVotedLocally) {
                    try {
                        val hasVotedInFirestore = voteRepository.hasUserVotedForMatch(currentUser.id, matchId).first()

                        // Se encontrou voto no Firestore, salva localmente para futuras verificações
                        if (hasVotedInFirestore) {
                            userPreferencesRepository.markMatchVoted(currentUser.id, matchId)
                            userHasVoted = true
                        }
                    } catch (e: Exception) {
                        // Se houver erro na verificação do Firestore, confiamos no registro local
                        println("Erro ao verificar votos do usuário no Firestore: ${e.message}")
                    }
                }

                // Obtém os resumos de votação dos jogadores
                val voteSummaries = try {
                    voteRepository.getMatchVoteSummary(matchId).first()
                } catch (e: Exception) {
                    println("Erro ao obter resumos de votação: ${e.message}")
                    emptyList()
                }

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
                    println("Erro ao observar resumos: ${e.message}")
                    // Não atualizamos o estado de erro para não sobrescrever a UI
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