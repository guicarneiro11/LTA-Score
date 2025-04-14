package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.android.data.repository.UserPreferencesRepository
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.usecases.GetMatchByIdUseCase
import com.guicarneirodev.ltascore.domain.usecases.SubmitPlayerVoteUseCase
import kotlinx.coroutines.delay
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
    private val submitPlayerVoteUseCase: SubmitPlayerVoteUseCase,
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VotingUiState())
    val uiState: StateFlow<VotingUiState> = _uiState.asStateFlow()

    fun loadMatch(matchId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

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
                    error = "Erro ao carregar partida: ${e.message}"
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
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)

            try {
                val match = _uiState.value.match
                val currentUser = userRepository.getCurrentUser().first()

                if (match != null && currentUser != null) {
                    // Usa o ID real do usuário autenticado
                    val userId = currentUser.id
                    var successCount = 0
                    var totalToSubmit = _uiState.value.ratings.size

                    // Envia cada avaliação
                    _uiState.value.ratings.forEach { (playerId, rating) ->
                        try {
                            submitPlayerVoteUseCase(
                                matchId = match.id,
                                playerId = playerId,
                                userId = userId,
                                rating = rating
                            )

                            // FALTANDO: Adicionar o voto ao histórico do usuário

                            successCount++
                        } catch (e: Exception) {
                            // Continua em caso de erro em um jogador específico
                        }
                    }

                    // MODIFICAÇÃO NECESSÁRIA: Marcar partida como votada no histórico
                    if (successCount > 0) {
                        // Marca no DataStore que o usuário votou nesta partida
                        userPreferencesRepository.markMatchVoted(userId, match.id)

                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            submitSuccess = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            error = "Falha ao enviar votos. Tente novamente."
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = "Usuário não autenticado ou partida não encontrada"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "Erro ao enviar votos: ${e.message}"
                )
            }
        }
    }
}