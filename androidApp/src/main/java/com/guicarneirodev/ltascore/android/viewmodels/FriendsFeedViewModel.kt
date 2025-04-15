package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.VoteComment
import com.guicarneirodev.ltascore.domain.models.VoteReaction
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteSocialRepository
import com.guicarneirodev.ltascore.domain.usecases.GetFriendsFeedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class VoteReactionsState(
    val reactions: List<VoteReaction> = emptyList(),
    val userReaction: VoteReaction? = null
)

data class FriendsFeedUiState(
    val isLoading: Boolean = false,
    val feed: List<FriendVoteHistoryItem> = emptyList(),
    val groupedFeed: Map<String, List<FriendVoteHistoryItem>> = emptyMap(),
    val voteReactions: Map<String, VoteReactionsState> = emptyMap(),
    val voteComments: Map<String, List<VoteComment>> = emptyMap(),
    val currentUserId: String = "",
    val error: String? = null
)

class FriendsFeedViewModel(
    private val getFriendsFeedUseCase: GetFriendsFeedUseCase,
    private val voteSocialRepository: VoteSocialRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsFeedUiState(isLoading = true))
    val uiState: StateFlow<FriendsFeedUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
        loadFeed()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser().first()
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        currentUserId = user.id
                    )
                }
            } catch (e: Exception) {
                // Ignorar erro
            }
        }
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                getFriendsFeedUseCase.execute().collectLatest { feedItems ->
                    // Agrupar por amigo + partida para melhor visualização
                    val groupedItems = feedItems.groupBy { item ->
                        // Formato: "Amigo: MatchId|Data"
                        "${item.friendUsername}: ${item.matchId}|${formatDateForGrouping(item.matchDate)}"
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        feed = feedItems,
                        groupedFeed = groupedItems
                    )

                    // Carregar reações e comentários para cada voto
                    feedItems.forEach { voteItem ->
                        loadReactionsForVote(voteItem.id)
                        loadCommentsForVote(voteItem.id)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar feed: ${e.message}"
                )
            }
        }
    }

    private fun loadReactionsForVote(voteId: String) {
        viewModelScope.launch {
            try {
                // Carregar todas as reações
                voteSocialRepository.getReactionsForVote(voteId).collectLatest { reactions ->
                    // Carregar a reação do usuário atual
                    val userReaction = voteSocialRepository.getUserReactionForVote(voteId).first()

                    // Atualizar o estado
                    val currentReactions = _uiState.value.voteReactions
                    val updatedReactions = currentReactions.toMutableMap().apply {
                        this[voteId] = VoteReactionsState(
                            reactions = reactions,
                            userReaction = userReaction
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        voteReactions = updatedReactions
                    )
                }
            } catch (e: Exception) {
                println("Erro ao carregar reações: ${e.message}")
            }
        }
    }

    private fun loadCommentsForVote(voteId: String) {
        viewModelScope.launch {
            try {
                voteSocialRepository.getCommentsForVote(voteId).collectLatest { comments ->
                    val currentComments = _uiState.value.voteComments
                    val updatedComments = currentComments.toMutableMap().apply {
                        this[voteId] = comments
                    }

                    _uiState.value = _uiState.value.copy(
                        voteComments = updatedComments
                    )
                }
            } catch (e: Exception) {
                println("Erro ao carregar comentários: ${e.message}")
            }
        }
    }

    fun addReaction(voteId: String, reaction: String) {
        viewModelScope.launch {
            try {
                voteSocialRepository.addReaction(voteId, reaction)
            } catch (e: Exception) {
                println("Erro ao adicionar reação: ${e.message}")
            }
        }
    }

    fun removeReaction(voteId: String) {
        viewModelScope.launch {
            try {
                voteSocialRepository.removeReaction(voteId)
            } catch (e: Exception) {
                println("Erro ao remover reação: ${e.message}")
            }
        }
    }

    fun addComment(voteId: String, text: String) {
        viewModelScope.launch {
            try {
                voteSocialRepository.addComment(voteId, text)
            } catch (e: Exception) {
                println("Erro ao adicionar comentário: ${e.message}")
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                voteSocialRepository.removeComment(commentId)
            } catch (e: Exception) {
                println("Erro ao remover comentário: ${e.message}")
            }
        }
    }

    private fun formatDateForGrouping(date: Instant): String {
        // Formatar data para agrupar por dia
        val localDate = date.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${localDate.dayOfMonth}/${localDate.monthNumber}/${localDate.year}"
    }
}