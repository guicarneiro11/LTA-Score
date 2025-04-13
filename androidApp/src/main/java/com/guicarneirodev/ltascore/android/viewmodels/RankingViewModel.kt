package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.data.datasource.static.PlayersStaticDataSource
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.PlayerRankingItem
import com.guicarneirodev.ltascore.domain.models.RankingFilter
import com.guicarneirodev.ltascore.domain.models.RankingFilterState
import com.guicarneirodev.ltascore.domain.usecases.GetPlayerRankingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Estado da UI da tela de Ranking
 */
data class RankingUiState(
    val isLoading: Boolean = false,
    val players: List<PlayerRankingItem> = emptyList(),
    val filteredPlayers: List<PlayerRankingItem> = emptyList(),
    val filterState: RankingFilterState = RankingFilterState(),
    val availableTeams: List<TeamFilterItem> = emptyList(),
    val error: String? = null,
    val searchQuery: String = ""
)

/**
 * Item para seleção de time nos filtros
 */
data class TeamFilterItem(
    val id: String,
    val name: String,
    val code: String,
    val imageUrl: String
)

/**
 * ViewModel para a tela de ranking de jogadores
 */
class RankingViewModel(
    private val getPlayerRankingUseCase: GetPlayerRankingUseCase,
    private val playersDataSource: PlayersStaticDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    init {
        loadTeamFilters()
        loadRanking()
    }

    /**
     * Carrega a lista de times disponíveis para filtro
     */
    private fun loadTeamFilters() {
        viewModelScope.launch {
            // Obter todos os jogadores
            val allPlayers = playersDataSource.getAllPlayers()

            // Extrair informações únicas dos times
            val teams = allPlayers
                .groupBy { it.teamId }
                .map { (teamId, players) ->
                    // Pegar o primeiro jogador para obter informações do time
                    players.first()

                    // Inferir código do time do ID (temporário, idealmente viria da API)
                    val teamCode = teamId.split("-").last().uppercase().take(3)

                    TeamFilterItem(
                        id = teamId,
                        name = teamId.split("-").joinToString(" ") { it.capitalize(Locale.ROOT) },
                        code = teamCode,
                        imageUrl = "" // Idealmente, buscar da API
                    )
                }
                .sortedBy { it.name }

            _uiState.value = _uiState.value.copy(
                availableTeams = teams
            )
        }
    }

    /**
     * Carrega o ranking de jogadores com base no filtro atual
     */
    fun loadRanking() {
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
                    error = "Erro ao carregar ranking: ${e.message}"
                )
            }
        }
    }

    /**
     * Atualiza o tipo de filtro
     */
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

    /**
     * Seleciona um time para filtrar
     */
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

    /**
     * Seleciona uma posição para filtrar
     */
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

    /**
     * Atualiza a busca por nome de jogador
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredPlayers = applySearch(_uiState.value.players, query)
        )
    }

    /**
     * Força uma atualização dos dados do ranking
     */
    fun refreshRanking() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Forçar atualização dos dados do ranking
                getPlayerRankingUseCase.refreshRanking()

                // Carregar dados com um intervalo maior para garantir melhor cobertura
                val largeLimitForRefresh = 100

                getPlayerRankingUseCase(_uiState.value.filterState, largeLimitForRefresh).collect { players ->
                    if (players.isEmpty()) {
                        // Se não há jogadores mesmo com limite maior, provavelmente há um problema
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Não foi possível carregar dados do ranking. Tente novamente."
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
                    error = "Erro ao atualizar ranking: ${e.message}"
                )
            }
        }
    }

    /**
     * Função auxiliar para aplicar filtro de busca
     */
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