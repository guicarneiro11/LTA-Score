package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.data.datasource.static.PlayersDataSource
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.Player
import com.guicarneirodev.ltascore.domain.repository.MatchPlayersRepository
import com.guicarneirodev.ltascore.domain.usecases.GetMatchByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AdminMatchPlayersUiState(
    val isLoading: Boolean = false,
    val match: Match? = null,
    val allPlayers: List<Player> = emptyList(),
    val selectedPlayerIds: List<String> = emptyList(),
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val error: String? = null
)

class AdminViewModel(
    private val getMatchByIdUseCase: GetMatchByIdUseCase,
    private val matchPlayersRepository: MatchPlayersRepository,
    private val playersDataSource: PlayersDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminMatchPlayersUiState(isLoading = true))
    val uiState: StateFlow<AdminMatchPlayersUiState> = _uiState.asStateFlow()

    fun loadMatch(matchId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, submitSuccess = false)

            try {
                val match = getMatchByIdUseCase(matchId).first()
                if (match == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Partida não encontrada"
                    )
                    return@launch
                }

                val allPlayers = mutableListOf<Player>()
                match.teams.forEach { team ->
                    val teamId = team.id
                    val teamPlayers = playersDataSource.getPlayersByTeamIdAndDate(
                        teamId,
                        match.startTime,
                        match.blockName
                    )
                    allPlayers.addAll(teamPlayers)
                }

                val participatingPlayerIds = matchPlayersRepository.getParticipatingPlayers(matchId).first()

                val selectedIds = if (participatingPlayerIds.isNotEmpty()) {
                    participatingPlayerIds
                } else {
                    val defaultSelectedIds = mutableListOf<String>()
                    match.teams.forEach { team ->
                        val positionsSelected = mutableSetOf<String>()
                        team.players.forEach { player ->
                            if (!positionsSelected.contains(player.position.name)) {
                                defaultSelectedIds.add(player.id)
                                positionsSelected.add(player.position.name)
                            }
                        }
                    }
                    defaultSelectedIds
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    match = match,
                    allPlayers = allPlayers,
                    selectedPlayerIds = selectedIds
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar partida: ${e.message}"
                )
            }
        }
    }

    fun updatePlayerSelection(playerId: String, isSelected: Boolean) {
        val currentSelection = _uiState.value.selectedPlayerIds.toMutableList()

        if (isSelected && !currentSelection.contains(playerId)) {
            currentSelection.add(playerId)
        } else if (!isSelected && currentSelection.contains(playerId)) {
            currentSelection.remove(playerId)
        }

        _uiState.value = _uiState.value.copy(selectedPlayerIds = currentSelection)
    }

    fun saveParticipatingPlayers(matchId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null, submitSuccess = false)

            try {
                val result = matchPlayersRepository.setParticipatingPlayers(
                    matchId,
                    _uiState.value.selectedPlayerIds
                )

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitSuccess = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = "Erro ao salvar: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "Erro ao salvar jogadores: ${e.message}"
                )
            }
        }
    }
}