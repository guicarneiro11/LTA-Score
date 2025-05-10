package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.android.data.repository.UserPreferencesRepository
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.usecases.GetMatchByIdUseCase
import com.guicarneirodev.ltascore.domain.usecases.SubmitPlayerVoteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.guicarneirodev.ltascore.android.R
import com.guicarneirodev.ltascore.android.util.StringResources
import com.guicarneirodev.ltascore.domain.repository.MatchPlayersRepository
import kotlinx.coroutines.delay

data class VotingUiState(
    val isLoading: Boolean = false,
    val match: Match? = null,
    val ratings: Map<String, Float> = emptyMap(),
    val participatingPlayerIds: List<String> = emptyList(),
    val allPlayersRated: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val error: String? = null,
    val saveParticipantsSuccess: Boolean = false
)

class VotingViewModel(
    private val getMatchByIdUseCase: GetMatchByIdUseCase,
    private val submitPlayerVoteUseCase: SubmitPlayerVoteUseCase,
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val matchPlayersRepository: MatchPlayersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VotingUiState())
    val uiState: StateFlow<VotingUiState> = _uiState.asStateFlow()

    fun loadMatch(matchId: String, isAdmin: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val match = getMatchByIdUseCase(matchId).first()

                if (match != null) {
                    val participatingPlayerIds = matchPlayersRepository.getParticipatingPlayers(matchId).first()

                    val finalMatch = if (!isAdmin && participatingPlayerIds.isNotEmpty()) {
                        val filteredTeams = match.teams.map { team ->
                            val filteredPlayers = team.players.filter { player ->
                                participatingPlayerIds.contains(player.id)
                            }
                            val playersToUse = if (filteredPlayers.isEmpty()) {
                                team.players
                            } else {
                                filteredPlayers
                            }
                            team.copy(players = playersToUse)
                        }
                        match.copy(teams = filteredTeams)
                    } else {
                        match
                    }

                    val initialRatings = mutableMapOf<String, Float>()
                    finalMatch.teams.forEach { team ->
                        team.players.forEach { player ->
                            initialRatings[player.id] = 0f
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        match = finalMatch,
                        ratings = initialRatings,
                        participatingPlayerIds = participatingPlayerIds
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = StringResources.getString(R.string.match_not_found)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.match_load_error, e.message ?: "")
                )
            }
        }
    }

    fun updatePlayerParticipation(playerId: String, isParticipating: Boolean) {
        val currentIds = _uiState.value.participatingPlayerIds.toMutableList()

        if (isParticipating && !currentIds.contains(playerId)) {
            currentIds.add(playerId)
        } else if (!isParticipating && currentIds.contains(playerId)) {
            currentIds.remove(playerId)
        }

        _uiState.value = _uiState.value.copy(
            participatingPlayerIds = currentIds
        )
    }

    fun saveParticipatingPlayers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSubmitting = true,
                error = null,
                saveParticipantsSuccess = false
            )

            try {
                val matchId = _uiState.value.match?.id ?: return@launch
                val playerIds = _uiState.value.participatingPlayerIds

                val result = matchPlayersRepository.setParticipatingPlayers(matchId, playerIds)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        saveParticipantsSuccess = true,
                        error = null
                    )

                    delay(2000)

                    _uiState.value = _uiState.value.copy(
                        saveParticipantsSuccess = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = "Erro ao salvar jogadores: ${result.exceptionOrNull()?.message}"
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

    fun updateRating(playerId: String, rating: Float) {
        val updatedRatings = _uiState.value.ratings.toMutableMap()
        updatedRatings[playerId] = rating

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
                    val userId = currentUser.id
                    var successCount = 0
                    _uiState.value.ratings.size

                    _uiState.value.ratings.forEach { (playerId, rating) ->
                        try {
                            submitPlayerVoteUseCase(
                                matchId = match.id,
                                playerId = playerId,
                                userId = userId,
                                rating = rating
                            )

                            successCount++
                        } catch (_: Exception) {
                        }
                    }

                    if (successCount > 0) {
                        userPreferencesRepository.markMatchVoted(userId, match.id)

                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            submitSuccess = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            error = StringResources.getString(R.string.vote_submit_fail)
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = StringResources.getString(R.string.not_authenticated_or_match)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = StringResources.getStringFormatted(R.string.vote_submit_error, e.message ?: "")
                )
            }
        }
    }
}