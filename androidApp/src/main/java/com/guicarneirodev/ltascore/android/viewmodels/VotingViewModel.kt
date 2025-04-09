package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.usecases.GetMatchByIdUseCase
import com.guicarneirodev.ltascore.domain.usecases.SubmitPlayerVoteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class VotingUiState(
    val isLoading: Boolean = false,
    val match: Match? = null,
    val ratings: Map<String, Float> = emptyMap(),
    val allPlayersRated: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val error: String? = null
)

class VotingViewModel(
    private val getMatchByIdUseCase: GetMatchByIdUseCase,
    private val submitPlayerVoteUseCase: SubmitPlayerVoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VotingUiState())
    val uiState: StateFlow<VotingUiState> = _uiState.asStateFlow()

    fun loadMatch(matchId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val match = getMatchByIdUseCase(matchId).first()
                if (match != null) {
                    // Inicializa as avaliações com zero
                    val initialRatings = mutableMapOf<String, Float>()
                    match.teams.forEach { team ->
                        team.players.forEach { player ->
                            initialRatings[player.id] = 0f
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        match = match,
                        ratings = initialRatings
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Partida não encontrada"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun updateRating(playerId: String, rating: Float) {
        val updatedRatings = _uiState.value.ratings.toMutableMap()
        updatedRatings[playerId] = rating

        // Verifica se todos os jogadores têm avaliação maior que zero
        val allRated = updatedRatings.all { (_, value) -> value > 0f }

        _uiState.value = _uiState.value.copy(
            ratings = updatedRatings,
            allPlayersRated = allRated
        )
    }

    fun submitAllRatings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)

            try {
                val match = _uiState.value.match
                if (match != null) {
                    val userId = "user_temp_id" // Temporário até implementar autenticação

                    // Envia cada avaliação
                    _uiState.value.ratings.forEach { (playerId, rating) ->
                        submitPlayerVoteUseCase(
                            matchId = match.id,
                            playerId = playerId,
                            userId = userId,
                            rating = rating
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = e.message
                )
            }
        }
    }
}