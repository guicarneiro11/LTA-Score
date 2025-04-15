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
                    println("Usuário atual carregado: ${user.id}")
                } else {
                    println("Nenhum usuário logado encontrado")
                }
            } catch (e: Exception) {
                println("Erro ao carregar usuário atual: ${e.message}")
            }
        }
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                getFriendsFeedUseCase.execute().collect { feedItems ->
                    println("Feed recebido: ${feedItems.size} itens")

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
                        println("Carregando reações e comentários para voto: ${voteItem.id}")
                        loadReactionsForVote(voteItem.id)
                        loadCommentsForVote(voteItem.id)
                    }
                }
            } catch (e: Exception) {
                println("Erro ao carregar feed: ${e.message}")
                e.printStackTrace()
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
                println("Iniciando carregamento de reações para: $voteId")

                // CORREÇÃO 1: Mudando de collectLatest para collect para evitar cancelamentos
                voteSocialRepository.getReactionsForVote(voteId).collect { reactions ->
                    println("Reações recebidas para $voteId: ${reactions.size}")

                    // Carregar a reação do usuário atual
                    val userReaction = voteSocialRepository.getUserReactionForVote(voteId).first()
                    println("Reação do usuário atual para $voteId: ${userReaction?.reaction ?: "nenhuma"}")

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
                println("Erro ao carregar reações para $voteId: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun loadCommentsForVote(voteId: String) {
        viewModelScope.launch {
            try {
                println("Iniciando carregamento de comentários para: $voteId")

                // CORREÇÃO 2: Mudando de collectLatest para collect para evitar cancelamentos
                voteSocialRepository.getCommentsForVote(voteId).collect { comments ->
                    println("Comentários recebidos para $voteId: ${comments.size}")

                    val currentComments = _uiState.value.voteComments
                    val updatedComments = currentComments.toMutableMap().apply {
                        this[voteId] = comments
                    }

                    _uiState.value = _uiState.value.copy(
                        voteComments = updatedComments
                    )
                }
            } catch (e: Exception) {
                println("Erro ao carregar comentários para $voteId: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // CORREÇÃO 3: Melhorar os métodos de ação para atualizar o estado local além de chamar o repositório
    fun addReaction(voteId: String, reaction: String) {
        viewModelScope.launch {
            try {
                println("Adicionando reação '$reaction' ao voto $voteId")

                // Realizar a operação no repositório
                val result = voteSocialRepository.addReaction(voteId, reaction)

                // CORREÇÃO: Atualizar o estado local imediatamente para feedback rápido
                result.onSuccess { newReaction ->
                    println("Reação adicionada com sucesso: ${newReaction.reaction}")

                    // Atualizar a lista de reações atual
                    val currentState = _uiState.value
                    val currentReactionsState = currentState.voteReactions[voteId] ?: VoteReactionsState()

                    // Removendo reação anterior do usuário (se existir)
                    val filteredReactions = currentReactionsState.reactions.filter {
                        it.userId != newReaction.userId
                    }

                    // Criando nova lista com a reação adicionada
                    val updatedReactions = filteredReactions + newReaction

                    // Atualizando o estado
                    val updatedReactionsMap = currentState.voteReactions.toMutableMap().apply {
                        this[voteId] = VoteReactionsState(
                            reactions = updatedReactions,
                            userReaction = newReaction
                        )
                    }

                    _uiState.value = currentState.copy(
                        voteReactions = updatedReactionsMap
                    )
                }.onFailure { error ->
                    println("Erro ao adicionar reação: ${error.message}")
                }
            } catch (e: Exception) {
                println("Erro ao adicionar reação: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun removeReaction(voteId: String) {
        viewModelScope.launch {
            try {
                println("Removendo reação do voto $voteId")

                // Salvar informação da reação atual antes de remover
                val currentState = _uiState.value
                val currentReactionsState = currentState.voteReactions[voteId] ?: VoteReactionsState()
                val userReaction = currentReactionsState.userReaction

                // Realizar a operação no repositório
                val result = voteSocialRepository.removeReaction(voteId)

                // CORREÇÃO: Atualizar o estado local imediatamente para feedback rápido
                result.onSuccess {
                    println("Reação removida com sucesso")

                    // Filtrar a reação removida da lista
                    val userId = userReaction?.userId ?: currentState.currentUserId
                    val updatedReactions = currentReactionsState.reactions.filter {
                        it.userId != userId
                    }

                    // Atualizando o estado
                    val updatedReactionsMap = currentState.voteReactions.toMutableMap().apply {
                        this[voteId] = VoteReactionsState(
                            reactions = updatedReactions,
                            userReaction = null
                        )
                    }

                    _uiState.value = currentState.copy(
                        voteReactions = updatedReactionsMap
                    )
                }.onFailure { error ->
                    println("Erro ao remover reação: ${error.message}")
                }
            } catch (e: Exception) {
                println("Erro ao remover reação: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun addComment(voteId: String, text: String) {
        viewModelScope.launch {
            try {
                println("Adicionando comentário ao voto $voteId: '$text'")

                // Realizar a operação no repositório
                val result = voteSocialRepository.addComment(voteId, text)

                // CORREÇÃO: Atualizar o estado local imediatamente para feedback rápido
                result.onSuccess { newComment ->
                    println("Comentário adicionado com sucesso: ${newComment.id}")

                    // Atualizar a lista de comentários
                    val currentState = _uiState.value
                    val currentComments = currentState.voteComments[voteId] ?: emptyList()
                    val updatedComments = currentComments + newComment

                    // Atualizando o estado
                    val updatedCommentsMap = currentState.voteComments.toMutableMap().apply {
                        this[voteId] = updatedComments
                    }

                    _uiState.value = currentState.copy(
                        voteComments = updatedCommentsMap
                    )
                }.onFailure { error ->
                    println("Erro ao adicionar comentário: ${error.message}")
                }
            } catch (e: Exception) {
                println("Erro ao adicionar comentário: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                println("Removendo comentário $commentId")

                // Realizar a operação no repositório
                val result = voteSocialRepository.removeComment(commentId)

                // CORREÇÃO: Atualizar o estado local imediatamente para feedback rápido
                result.onSuccess {
                    println("Comentário removido com sucesso")

                    // Atualizar todas as listas de comentários
                    val currentState = _uiState.value
                    val updatedCommentsMap = currentState.voteComments.mapValues { (_, comments) ->
                        comments.filter { it.id != commentId }
                    }.toMutableMap()

                    _uiState.value = currentState.copy(
                        voteComments = updatedCommentsMap
                    )
                }.onFailure { error ->
                    println("Erro ao remover comentário: ${error.message}")
                }
            } catch (e: Exception) {
                println("Erro ao remover comentário: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun formatDateForGrouping(date: Instant): String {
        // Formatar data para agrupar por dia
        val localDate = date.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${localDate.dayOfMonth}/${localDate.monthNumber}/${localDate.year}"
    }
}