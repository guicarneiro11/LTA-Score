package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.data.datasource.static.PlayersStaticDataSource
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val availableTeams: List<TeamFilterItem> = emptyList(),
    val selectedTeamId: String? = null,
    val error: String? = null,
    val success: String? = null
)

class EditProfileViewModel(
    private val userRepository: UserRepository,
    private val matchRepository: MatchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            loadUserProfile()

            loadAvailableTeams()
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser().first()
                if (currentUser != null) {
                    _uiState.value = _uiState.value.copy(
                        selectedTeamId = currentUser.favoriteTeamId
                    )
                    println("Time favorito carregado: ${currentUser.favoriteTeamId}")
                }
            } catch (e: Exception) {
                println("Erro ao carregar perfil: ${e.message}")
            }
        }
    }

    private fun loadAvailableTeams() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val matches = matchRepository.getMatches("lta_s").first()

                val teamMap = mutableMapOf<String, TeamFilterItem>()

                matches.forEach { match ->
                    match.teams.forEach { team ->
                        if (!teamMap.containsKey(team.id)) {
                            teamMap[team.id] = TeamFilterItem(
                                id = team.id,
                                name = team.name,
                                code = team.code,
                                imageUrl = team.imageUrl
                            )
                        }
                    }
                }

                val ltaSulTeams = listOf(
                    teamMap["loud"] ?: TeamFilterItem("loud", "LOUD", "LOUD", ""),
                    teamMap["pain-gaming"] ?: TeamFilterItem("pain-gaming", "paiN Gaming", "PAIN", ""),
                    teamMap["isurus-estral"] ?: TeamFilterItem("isurus-estral", "Isurus Estral", "IE", ""),
                    teamMap["leviatan"] ?: TeamFilterItem("leviatan", "LEVIATÁN", "LEV", ""),
                    teamMap["furia"] ?: TeamFilterItem("furia", "FURIA", "FUR", ""),
                    teamMap["keyd"] ?: TeamFilterItem("keyd", "Keyd Stars", "VKS", ""),
                    teamMap["red"] ?: TeamFilterItem("red", "RED Canids", "RED", ""),
                    teamMap["fxw7"] ?: TeamFilterItem("fxw7", "Fluxo W7M", "FXW7", "")
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    availableTeams = ltaSulTeams
                )
            } catch (e: Exception) {
                val ltaSulTeams = listOf(
                    TeamFilterItem("loud", "LOUD", "LOUD", ""),
                    TeamFilterItem("pain-gaming", "paiN Gaming", "PAIN", ""),
                    TeamFilterItem("isurus-estral", "Isurus Estral", "IE", ""),
                    TeamFilterItem("leviatan", "LEVIATÁN", "LEV", ""),
                    TeamFilterItem("furia", "FURIA", "FUR", ""),
                    TeamFilterItem("keyd", "Keyd Stars", "VKS", ""),
                    TeamFilterItem("red", "RED Canids", "RED", ""),
                    TeamFilterItem("fxw7", "Fluxo W7M", "FXW7", "")
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    availableTeams = ltaSulTeams,
                    error = "Não foi possível carregar os logos dos times: ${e.message}"
                )
            }
        }
    }

    fun selectTeam(teamId: String) {
        _uiState.value = _uiState.value.copy(selectedTeamId = teamId)
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                success = null
            )

            try {
                val teamId = _uiState.value.selectedTeamId
                if (teamId != null) {
                    val result = userRepository.updateFavoriteTeam(teamId)
                    result.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                success = "Time favorito atualizado com sucesso"
                            )
                        },
                        onFailure = { e ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Erro ao atualizar time favorito: ${e.message}"
                            )
                        }
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Selecione um time favorito"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao salvar perfil: ${e.message}"
                )
            }
        }
    }
}