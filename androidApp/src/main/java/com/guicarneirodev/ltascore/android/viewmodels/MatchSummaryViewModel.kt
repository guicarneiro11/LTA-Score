package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.android.data.repository.UserPreferencesRepository
import com.guicarneirodev.ltascore.data.datasource.static.PlayersDataSource
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.VoteSummary
import com.guicarneirodev.ltascore.domain.repository.MatchPlayersRepository
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

class MatchSummaryViewModel(
    private val getMatchByIdUseCase: GetMatchByIdUseCase,
    private val voteRepository: VoteRepository,
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val matchPlayersRepository: MatchPlayersRepository,
    private val playersDataSource: PlayersDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchSummaryUiState(isLoading = true))
    val uiState: StateFlow<MatchSummaryUiState> = _uiState.asStateFlow()

    fun loadMatch(matchId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                delay(500)

                val currentUser = userRepository.getCurrentUser().first()

                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuário não autenticado"
                    )
                    return@launch
                }

                var match: Match? = null
                var attempts = 0
                while (match == null && attempts < 3) {
                    match = getMatchByIdUseCase(matchId).first()
                    if (match == null) {
                        delay(500)
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

                val participatingPlayerIds = matchPlayersRepository.getParticipatingPlayers(matchId).first()

                val finalMatch = if (participatingPlayerIds.isNotEmpty()) {
                    val filteredTeams = match.teams.map { team ->
                        val allTeamPlayers = playersDataSource.getAllPlayersByTeamId(team.id)

                        val selectedPlayers = allTeamPlayers.filter { player ->
                            participatingPlayerIds.contains(player.id)
                        }

                        val playersToUse = selectedPlayers.ifEmpty {
                            team.players
                        }

                        team.copy(players = playersToUse)
                    }
                    match.copy(teams = filteredTeams)
                } else {
                    match
                }

                val hasVotedLocally = userPreferencesRepository.hasUserVotedForMatch(currentUser.id, matchId).first()

                var userHasVoted = hasVotedLocally
                if (!hasVotedLocally) {
                    try {
                        val hasVotedInFirestore = voteRepository.hasUserVotedForMatch(currentUser.id, matchId).first()

                        if (hasVotedInFirestore) {
                            userPreferencesRepository.markMatchVoted(currentUser.id, matchId)
                            userHasVoted = true
                        }
                    } catch (e: Exception) {
                        println("Erro ao verificar votos do usuário no Firestore: ${e.message}")
                    }
                }

                val voteSummaries = try {
                    voteRepository.getMatchVoteSummary(matchId).first()
                } catch (e: Exception) {
                    println("Erro ao obter resumos de votação: ${e.message}")
                    emptyList()
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    match = finalMatch,
                    voteSummaries = voteSummaries,
                    userHasVoted = userHasVoted
                )

                observeVoteSummaries(matchId)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar dados: ${e.message}"
                )
            }
        }
    }

    private fun observeVoteSummaries(matchId: String) {
        viewModelScope.launch {
            voteRepository.getMatchVoteSummary(matchId)
                .catch { e ->
                    println("Erro ao observar resumos: ${e.message}")
                }
                .collect { summaries ->
                    _uiState.value = _uiState.value.copy(
                        voteSummaries = summaries
                    )
                }
        }
    }
}