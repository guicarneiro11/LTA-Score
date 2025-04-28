package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.android.data.cache.FavoriteTeamCache
import com.guicarneirodev.ltascore.android.data.cache.UserEvents
import com.guicarneirodev.ltascore.android.util.StringResources
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.guicarneirodev.ltascore.android.R

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val availableTeams: List<TeamFilterItem> = emptyList(),
    val selectedTeamId: String? = null,
    val error: String? = null,
    val success: String? = null
)

class EditProfileViewModel(
    private val userRepository: UserRepository,
    private val matchRepository: MatchRepository,
    private val authViewModel: AuthViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUserProfile()
    }

    fun loadTeams() {
        loadAvailableTeams()
    }

    private fun loadCurrentUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val currentUser = userRepository.getCurrentUser().first()

                _uiState.value = _uiState.value.copy(
                    selectedTeamId = currentUser?.favoriteTeamId,
                    isLoading = false
                )

                loadAvailableTeams()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.profile_load_error, e.message ?: "")
                )
            }
        }
    }

    internal fun loadAvailableTeams() {
        viewModelScope.launch {
            if (_uiState.value.availableTeams.isNotEmpty()) {
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val ltaSulTeamsDefault = listOf(
                    TeamFilterItem("loud", "LOUD", "LOUD", ""),
                    TeamFilterItem("pain-gaming", "paiN Gaming", "PAIN", ""),
                    TeamFilterItem("isurus-estral", "Isurus Estral", "IE", ""),
                    TeamFilterItem("leviatan", "LEVIATÁN", "LEV", ""),
                    TeamFilterItem("furia", "FURIA", "FUR", ""),
                    TeamFilterItem("keyd", "Vivo Keyd Stars", "VKS", ""),
                    TeamFilterItem("red", "RED Kalunga", "RED", ""),
                    TeamFilterItem("fxw7", "Fluxo W7M", "FXW7", "")
                )

                val ltaNorteTeamsDefault = listOf(
                    TeamFilterItem("cloud9-kia", "Cloud9 KIA", "C9", ""),
                    TeamFilterItem("100-thieves", "100 Thieves", "100T", ""),
                    TeamFilterItem("flyquest", "FlyQuest", "FLY", ""),
                    TeamFilterItem("team-liquid-honda", "Team Liquid Honda", "TL", ""),
                    TeamFilterItem("shopify-rebellion", "Shopify Rebellion", "SR", ""),
                    TeamFilterItem("dignitas", "Dignitas", "DIG", ""),
                    TeamFilterItem("lyon", "Lyon", "LYON", ""),
                    TeamFilterItem("disguised", "Disguised", "DSG", "")
                )

                val teamMap = mutableMapOf<String, TeamFilterItem>()

                try {
                    val matchesSul = matchRepository.getMatches("lta_s").first()

                    matchesSul.forEach { match ->
                        match.teams.forEach { team ->
                            val internalId = when(team.code) {
                                "LOUD" -> "loud"
                                "PAIN" -> "pain-gaming"
                                "IE" -> "isurus-estral"
                                "LEV" -> "leviatan"
                                "FUR" -> "furia"
                                "VKS" -> "keyd"
                                "RED" -> "red"
                                "FXW7" -> "fxw7"
                                else -> team.id
                            }

                            if (!teamMap.containsKey(internalId)) {
                                teamMap[internalId] = TeamFilterItem(
                                    id = internalId,
                                    name = team.name,
                                    code = team.code,
                                    imageUrl = team.imageUrl
                                )
                            }
                        }
                    }

                    val matchesNorte = matchRepository.getMatches("lta_n").first()

                    matchesNorte.forEach { match ->
                        match.teams.forEach { team ->
                            val internalId = when(team.code) {
                                "C9" -> "cloud9-kia"
                                "100T" -> "100-thieves"
                                "FLY" -> "flyquest"
                                "TL" -> "team-liquid-honda"
                                "SR" -> "shopify-rebellion"
                                "DIG" -> "dignitas"
                                "LYON" -> "lyon"
                                "DSG" -> "disguised"
                                else -> team.id
                            }

                            if (!teamMap.containsKey(internalId)) {
                                teamMap[internalId] = TeamFilterItem(
                                    id = internalId,
                                    name = team.name,
                                    code = team.code,
                                    imageUrl = team.imageUrl
                                )
                            }
                        }
                    }

                    val ltaSulTeams = ltaSulTeamsDefault.map { defaultTeam ->
                        teamMap[defaultTeam.id] ?: defaultTeam
                    }

                    val ltaNorteTeams = ltaNorteTeamsDefault.map { defaultTeam ->
                        teamMap[defaultTeam.id] ?: defaultTeam
                    }

                    val allTeams = ltaSulTeams + ltaNorteTeams

                    _uiState.value = _uiState.value.copy(
                        availableTeams = allTeams,
                        isLoading = false
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = StringResources.getStringFormatted(R.string.team_load_error, e.message ?: "")
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.team_load_error, e.message ?: "")
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
                            FavoriteTeamCache.updateFavoriteTeam(teamId)

                            userRepository.refreshCurrentUser()

                            viewModelScope.launch {
                                delay(200)
                                val userId = try {
                                    userRepository.getCurrentUser().first()?.id ?: ""
                                } catch (_: Exception) {
                                    ""
                                }

                                UserEvents.notifyUserUpdated(userId)

                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    success = StringResources.getString(R.string.team_updated)
                                )
                            }
                        },
                        onFailure = { e ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = StringResources.getStringFormatted(R.string.team_save_error, e.message ?: "")
                            )
                        }
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = StringResources.getString(R.string.select_team_required)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.profile_save_error, e.message ?: "")
                )
            }
        }
    }

    fun finishSaving(teamId: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            FavoriteTeamCache.updateFavoriteTeam(teamId)

            userRepository.refreshCurrentUser()

            try {
                val userId = userRepository.getCurrentUser().first()?.id ?: ""
                UserEvents.notifyUserUpdated(userId)

                delay(300)

                _uiState.value = _uiState.value.copy(
                    success = StringResources.getString(R.string.team_updated)
                )

                onComplete(teamId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = StringResources.getStringFormatted(R.string.team_save_error, e.message ?: "")
                )
            }
        }
    }
}