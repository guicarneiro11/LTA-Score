package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.MatchState
import com.guicarneirodev.ltascore.domain.usecases.GetMatchesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MatchFilter {
    ALL, UPCOMING, COMPLETED
}

data class League(val name: String, val slug: String)

data class MatchesUiState(
    val isLoading: Boolean = false,
    val matches: List<Match> = emptyList(),
    val filteredMatches: List<Match> = emptyList(),
    val filter: MatchFilter = MatchFilter.ALL,
    val availableLeagues: List<League> = listOf(
        League("LTA Sul", "lta_s"),
        League("LTA Norte", "lta_n")
    ),
    val selectedLeagueIndex: Int = 0,
    val error: String? = null
)

class MatchesViewModel(
    private val getMatchesUseCase: GetMatchesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchesUiState())
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    init {
        loadMatches()
    }

    fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val leagueSlug = _uiState.value.availableLeagues[_uiState.value.selectedLeagueIndex].slug
                println("ViewModel: Carregando partidas para liga $leagueSlug")

                getMatchesUseCase(leagueSlug).collect { matches ->
                    println("ViewModel: Recebidas ${matches.size} partidas")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        matches = matches,
                        filteredMatches = filterMatches(matches, _uiState.value.filter),
                        error = if (matches.isEmpty()) "Nenhuma partida encontrada" else null
                    )
                }
            } catch (e: Exception) {
                println("ViewModel: Erro ao carregar partidas: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar partidas: ${e.message}"
                )
            }
        }
    }

    fun selectLeague(index: Int) {
        if (index != _uiState.value.selectedLeagueIndex) {
            _uiState.value = _uiState.value.copy(selectedLeagueIndex = index)
            loadMatches()
        }
    }

    fun setFilter(filter: MatchFilter) {
        if (filter != _uiState.value.filter) {
            _uiState.value = _uiState.value.copy(
                filter = filter,
                filteredMatches = filterMatches(_uiState.value.matches, filter)
            )
        }
    }

    private fun filterMatches(matches: List<Match>, filter: MatchFilter): List<Match> {
        return when (filter) {
            MatchFilter.ALL -> matches
            MatchFilter.UPCOMING -> matches.filter { it.state == MatchState.UNSTARTED }
            MatchFilter.COMPLETED -> matches.filter { it.state == MatchState.COMPLETED }
        }
    }
}