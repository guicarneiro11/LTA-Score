package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.android.util.StringResources
import com.guicarneirodev.ltascore.api.LoLEsportsApi
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.MatchState
import com.guicarneirodev.ltascore.domain.usecases.GetMatchesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.guicarneirodev.ltascore.android.R

enum class MatchFilter {
    ALL, UPCOMING, LIVE, COMPLETED
}

data class League(val name: String, val slug: String)

data class MatchesUiState(
    val isLoading: Boolean = false,
    val matches: List<Match> = emptyList(),
    val filteredMatches: List<Match> = emptyList(),
    val filter: MatchFilter = MatchFilter.ALL,
    val availableLeagues: List<League> = listOf(
        League("LTA South", "lta_s"),
        League("LTA North", "lta_n"),
        League("Circuito Desafiante", "cd")
    ),
    val selectedLeagueIndex: Int = 0,
    val error: String? = null,
    val ltaCrossLogo: String? = null
)

class MatchesViewModel(
    private val getMatchesUseCase: GetMatchesUseCase,
    private val loLEsportsApi: LoLEsportsApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchesUiState())
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    init {
        loadMatches()
        loadLtaCrossLogo()
    }

    private fun loadLtaCrossLogo() {
        viewModelScope.launch {
            try {
                val response = loLEsportsApi.getLeagues()
                val crossLeague = response.data?.leagues?.find { it.slug == "lta_cross" }
                crossLeague?.let { league ->
                    val secureImageUrl = league.image.replace("http://", "https://")
                    _uiState.value = _uiState.value.copy(ltaCrossLogo = secureImageUrl)
                }
            } catch (e: Exception) {
                println("Erro ao carregar logo LTA Cross: ${e.message}")
            }
        }
    }

    fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val leagueSlug = _uiState.value.availableLeagues[_uiState.value.selectedLeagueIndex].slug
                println("ViewModel: Carregando partidas para liga $leagueSlug")

                getMatchesUseCase(leagueSlug).collect { matches ->
                    println("ViewModel: Recebidas ${matches.size} partidas")

                    val matchesWithVods = matches.filter { it.vodUrl != null }
                    println("ViewModel: Partidas com VODs: ${matchesWithVods.size}")
                    matchesWithVods.forEach { match ->
                        println("Partida ${match.id} tem VOD: ${match.vodUrl}")
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        matches = matches,
                        filteredMatches = filterMatches(matches, _uiState.value.filter),
                        error = if (matches.isEmpty()) StringResources.getString(R.string.no_match_found) else null
                    )
                }
            } catch (e: Exception) {
                println("ViewModel: Erro ao carregar partidas: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.matches_load_error, e.message ?: "")
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
            MatchFilter.LIVE -> matches.filter { it.state == MatchState.INPROGRESS }
            MatchFilter.COMPLETED -> matches.filter { it.state == MatchState.COMPLETED }
        }
    }
}