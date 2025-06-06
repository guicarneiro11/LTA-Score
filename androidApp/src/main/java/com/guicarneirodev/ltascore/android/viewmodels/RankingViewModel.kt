package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.android.util.StringResources
import com.guicarneirodev.ltascore.data.datasource.static.PlayersDataSource
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.PlayerRankingItem
import com.guicarneirodev.ltascore.domain.models.RankingFilter
import com.guicarneirodev.ltascore.domain.models.RankingFilterState
import com.guicarneirodev.ltascore.domain.usecases.GetPlayerRankingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.guicarneirodev.ltascore.android.R
import com.guicarneirodev.ltascore.domain.models.TeamFilterItem

data class RankingUiState(
    val isLoading: Boolean = false,
    val players: List<PlayerRankingItem> = emptyList(),
    val filteredPlayers: List<PlayerRankingItem> = emptyList(),
    val filterState: RankingFilterState = RankingFilterState(),
    val availableTeams: List<TeamFilterItem> = emptyList(),
    val error: String? = null,
    val searchQuery: String = ""
)

class RankingViewModel(
    private val getPlayerRankingUseCase: GetPlayerRankingUseCase,
    private val playersDataSource: PlayersDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    init {
        loadTeamFilters()
        loadRanking()
    }

    private fun loadTeamFilters() {
        viewModelScope.launch {
            val allPlayers = playersDataSource.getAllPlayers()

            val teams = allPlayers
                .groupBy { it.teamId }
                .map { (teamId, players) ->
                    players.first()

                    val teamCode = teamId.split("-").last().uppercase().take(3)

                    TeamFilterItem(
                        id = teamId,
                        name = teamId.split("-").joinToString(" ") {
                            it.replaceFirstChar { char -> char.titlecase() }
                        },
                        code = teamCode,
                        imageUrl = ""
                    )
                }
                .sortedBy { it.name }

            _uiState.value = _uiState.value.copy(
                availableTeams = teams
            )
        }
    }

    private fun loadRanking() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                getPlayerRankingUseCase(_uiState.value.filterState).collect { players ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        players = players,
                        filteredPlayers = applySearch(players, _uiState.value.searchQuery)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.ranking_load_error, e.message ?: "")
                )
            }
        }
    }

    fun setFilterType(filter: RankingFilter) {
        if (filter != _uiState.value.filterState.currentFilter) {
            _uiState.value = _uiState.value.copy(
                filterState = _uiState.value.filterState.copy(
                    currentFilter = filter
                )
            )
            loadRanking()
        }
    }

    fun selectTeam(teamId: String?) {
        if (teamId != _uiState.value.filterState.selectedTeamId) {
            _uiState.value = _uiState.value.copy(
                filterState = _uiState.value.filterState.copy(
                    currentFilter = RankingFilter.BY_TEAM,
                    selectedTeamId = teamId
                )
            )
            loadRanking()
        }
    }

    fun selectPosition(position: PlayerPosition?) {
        if (position != _uiState.value.filterState.selectedPosition) {
            _uiState.value = _uiState.value.copy(
                filterState = _uiState.value.filterState.copy(
                    currentFilter = RankingFilter.BY_POSITION,
                    selectedPosition = position
                )
            )
            loadRanking()
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredPlayers = applySearch(_uiState.value.players, query)
        )
    }

    fun refreshRanking() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                getPlayerRankingUseCase.refreshRanking()

                val largeLimitForRefresh = 100

                getPlayerRankingUseCase(_uiState.value.filterState, largeLimitForRefresh).collect { players ->
                    if (players.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = StringResources.getString(R.string.ranking_data_error)
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            players = players,
                            filteredPlayers = applySearch(players, _uiState.value.searchQuery)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.ranking_update_error, e.message ?: "")
                )
            }
        }
    }

    private fun applySearch(players: List<PlayerRankingItem>, query: String): List<PlayerRankingItem> {
        if (query.isBlank()) return players

        val lowerQuery = query.lowercase()
        return players.filter { player ->
            player.player.name.lowercase().contains(lowerQuery) ||
                    player.player.nickname.lowercase().contains(lowerQuery) ||
                    player.teamName.lowercase().contains(lowerQuery) ||
                    player.teamCode.lowercase().contains(lowerQuery)
        }
    }
}