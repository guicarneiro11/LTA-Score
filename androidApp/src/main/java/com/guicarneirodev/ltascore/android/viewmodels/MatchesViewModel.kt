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
import com.guicarneirodev.ltascore.domain.models.MatchPredictionStats
import com.guicarneirodev.ltascore.domain.usecases.ManageMatchPredictionsUseCase

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
    val ltaCrossLogo: String? = null,
    val splitTitle: String = "Split 2 – 2025",
    val matchPredictions: Map<String, MatchPredictionStats> = emptyMap(),
    val userPredictions: Map<String, String> = emptyMap(),
    val predictionsLoading: Map<String, Boolean> = emptyMap()
)

class MatchesViewModel(
    private val getMatchesUseCase: GetMatchesUseCase,
    private val loLEsportsApi: LoLEsportsApi,
    private val manageMatchPredictionsUseCase: ManageMatchPredictionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchesUiState())
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    init {
        val initialLeagueSlug = _uiState.value.availableLeagues[_uiState.value.selectedLeagueIndex].slug
        val initialSplitTitle = if (initialLeagueSlug == "cd") "Split 1 – 2025" else "Split 2 – 2025"

        _uiState.value = _uiState.value.copy(splitTitle = initialSplitTitle)

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

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        matches = matches,
                        filteredMatches = filterMatches(matches, _uiState.value.filter),
                        error = if (matches.isEmpty()) StringResources.getString(R.string.no_match_found) else null
                    )

                    val upcomingMatches = matches.filter { it.state == MatchState.UNSTARTED }
                    upcomingMatches.forEach { match ->
                        loadMatchPredictionStats(match.id)
                        loadUserPrediction(match.id)
                    }
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

    fun getVodUrlForMatch(matchId: String, callback: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val eventDetails = loLEsportsApi.getMatch(matchId)

                val event = eventDetails.data?.event

                if (event?.match?.games?.isNotEmpty() == true) {
                    val game = event.match.games.firstOrNull()

                    if (game?.vods?.isNotEmpty() == true) {
                        val ptBrVod = game.vods.find { it.locale == "pt-BR" }

                        if (ptBrVod != null && ptBrVod.parameter != null) {
                            val youtubeUrl = "https://www.youtube.com/watch?v=${ptBrVod.parameter}"
                            callback(youtubeUrl)
                            return@launch
                        } else {
                            val anyVod = game.vods.firstOrNull()
                            if (anyVod != null && anyVod.parameter != null) {
                                val youtubeUrl = "https://www.youtube.com/watch?v=${anyVod.parameter}"
                                callback(youtubeUrl)
                                return@launch
                            }
                        }
                    }
                }

                println("Não foi possível encontrar o VOD para a partida: $matchId")
                callback(null)

            } catch (e: Exception) {
                println("Erro ao buscar detalhes do evento: ${e.message}")
                e.printStackTrace()
                callback(null)
            }
        }
    }

    fun selectLeague(index: Int) {
        if (index != _uiState.value.selectedLeagueIndex) {
            val leagueSlug = _uiState.value.availableLeagues[index].slug
            val splitTitle = when (leagueSlug) {
                "cd" -> "Split 1 – 2025"
                else -> "Split 2 – 2025"
            }

            _uiState.value = _uiState.value.copy(
                selectedLeagueIndex = index,
                splitTitle = splitTitle
            )
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

    fun submitPrediction(matchId: String, teamId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                predictionsLoading = _uiState.value.predictionsLoading + (matchId to true)
            )

            try {
                manageMatchPredictionsUseCase.submitPrediction(matchId, teamId).fold(
                    onSuccess = {
                        loadUserPrediction(matchId)

                        loadMatchPredictionStats(matchId)

                        _uiState.value = _uiState.value.copy(
                            predictionsLoading = _uiState.value.predictionsLoading + (matchId to false)
                        )
                    },
                    onFailure = { error ->
                        println("Erro ao submeter palpite: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            predictionsLoading = _uiState.value.predictionsLoading + (matchId to false)
                        )
                    }
                )
            } catch (e: Exception) {
                println("Erro ao submeter palpite: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    predictionsLoading = _uiState.value.predictionsLoading + (matchId to false)
                )
            }
        }
    }

    fun loadMatchPredictionStats(matchId: String) {
        viewModelScope.launch {
            manageMatchPredictionsUseCase.getMatchPredictionStats(matchId).collect { stats ->
                _uiState.value = _uiState.value.copy(
                    matchPredictions = _uiState.value.matchPredictions + (matchId to stats)
                )
            }
        }
    }

    fun loadUserPrediction(matchId: String) {
        viewModelScope.launch {
            manageMatchPredictionsUseCase.getUserPrediction(matchId).collect { prediction ->
                if (prediction != null) {
                    _uiState.value = _uiState.value.copy(
                        userPredictions = _uiState.value.userPredictions + (matchId to prediction.predictedTeamId)
                    )
                } else {
                    val updatedPredictions = _uiState.value.userPredictions.toMutableMap()
                    updatedPredictions.remove(matchId)

                    _uiState.value = _uiState.value.copy(
                        userPredictions = updatedPredictions
                    )
                }
            }
        }
    }
}