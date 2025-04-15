package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.android.data.cache.FeedCache
import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.VoteComment
import com.guicarneirodev.ltascore.domain.models.VoteReaction
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteSocialRepository
import com.guicarneirodev.ltascore.domain.usecases.GetFriendsFeedUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

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

    // Usado para controlar os jobs de coleta de dados do Firebase
    private var reactionsJobs = mutableMapOf<String, Job>()
    private var commentsJobs = mutableMapOf<String, Job>()

    // SupervisorJob para manter as coletas ativas mesmo se algumas falharem
    private val supervisorJob = SupervisorJob()

    init {
        loadCurrentUser()

        // Inicializa o estado com dados do cache, se disponíveis
        initializeFromCache()

        // Carrega os dados mais recentes
        loadFeed()
    }

    private fun initializeFromCache() {
        viewModelScope.launch {
            // Atualizar o ID do usuário atual (importante fazer isso primeiro)
            val user = userRepository.getCurrentUser().first()
            if (user != null) {
                _uiState.value = _uiState.value.copy(currentUserId = user.id)
            }

            // Combina todos os fluxos do cache e atualiza o estado de uma vez
            combine(
                FeedCache.cachedFeed,
                FeedCache.cachedReactions,
                FeedCache.cachedUserReactions,
                FeedCache.cachedComments
            ) { feed, reactions, userReactions, comments ->
                if (feed.isNotEmpty()) {
                    // Agrupar por amigo + partida para exibição
                    val groupedItems = feed.groupBy { item ->
                        "${item.friendUsername}: ${item.matchId}|${formatDateForGrouping(item.matchDate)}"
                    }

                    // Criar o mapa de estados de reação
                    val reactionsState = reactions.mapValues { (voteId, reactionsList) ->
                        VoteReactionsState(
                            reactions = reactionsList,
                            userReaction = userReactions[voteId]
                        )
                    }

                    // Atualizar o estado da UI com todos os dados do cache
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        feed = feed,
                        groupedFeed = groupedItems,
                        voteReactions = reactionsState,
                        voteComments = comments
                    )
                }
            }.collect()
        }
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

                    // Armazenar no cache
                    FeedCache.updateFeed(feedItems)

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

                    // Cancela jobs anteriores antes de iniciar novos
                    cancelExistingJobs()

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

    private fun cancelExistingJobs() {
        reactionsJobs.forEach { (_, job) -> job.cancel() }
        commentsJobs.forEach { (_, job) -> job.cancel() }
        reactionsJobs.clear()
        commentsJobs.clear()
    }

    private fun loadReactionsForVote(voteId: String) {
        // Inicia um novo job para coletar reações e o armazena no mapa
        reactionsJobs[voteId] = viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            try {
                println("Iniciando carregamento de reações para: $voteId")

                // Coletar reações do repositório
                voteSocialRepository.getReactionsForVote(voteId).collect { reactions ->
                    println("Reações recebidas para $voteId: ${reactions.size}")

                    // Armazenar no cache
                    FeedCache.updateReactions(voteId, reactions)

                    // Carregar a reação do usuário atual
                    val userReaction = withContext(Dispatchers.IO) {
                        voteSocialRepository.getUserReactionForVote(voteId).first()
                    }

                    // Armazenar no cache
                    FeedCache.updateUserReaction(voteId, userReaction)

                    println("Reação do usuário atual para $voteId: ${userReaction?.reaction ?: "nenhuma"}")

                    withContext(Dispatchers.Main) {
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
                }
            } catch (e: Exception) {
                println("Erro ao carregar reações para $voteId: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun loadCommentsForVote(voteId: String) {
        // Inicia um novo job para coletar comentários e o armazena no mapa
        commentsJobs[voteId] = viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            try {
                println("Iniciando carregamento de comentários para: $voteId")

                // Coletar comentários do repositório
                voteSocialRepository.getCommentsForVote(voteId).collect { comments ->
                    println("Comentários recebidos para $voteId: ${comments.size}")

                    // Armazenar no cache
                    FeedCache.updateComments(voteId, comments)

                    withContext(Dispatchers.Main) {
                        // Atualizar o estado
                        val currentComments = _uiState.value.voteComments
                        val updatedComments = currentComments.toMutableMap().apply {
                            this[voteId] = comments
                        }

                        _uiState.value = _uiState.value.copy(
                            voteComments = updatedComments
                        )
                    }
                }
            } catch (e: Exception) {
                println("Erro ao carregar comentários para $voteId: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun addReaction(voteId: String, reaction: String) {
        viewModelScope.launch {
            try {
                println("Adicionando reação '$reaction' ao voto $voteId")

                // Realizar a operação no repositório
                val result = voteSocialRepository.addReaction(voteId, reaction)

                // Atualizar o estado local imediatamente para feedback rápido
                result.onSuccess { newReaction ->
                    println("Reação adicionada com sucesso: ${newReaction.reaction}")

                    // Atualizar o cache
                    FeedCache.addOrUpdateReaction(voteId, newReaction)

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

                // Obter o ID do usuário atual
                val currentUserId = _uiState.value.currentUserId

                // Salvar informação da reação atual antes de remover
                val currentState = _uiState.value
                val currentReactionsState = currentState.voteReactions[voteId] ?: VoteReactionsState()
                val userReaction = currentReactionsState.userReaction

                // Realizar a operação no repositório
                val result = voteSocialRepository.removeReaction(voteId)

                // Atualizar o estado local imediatamente para feedback rápido
                result.onSuccess {
                    println("Reação removida com sucesso")

                    // Atualizar o cache
                    FeedCache.removeReaction(voteId, currentUserId)

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

                // Atualizar o estado local imediatamente para feedback rápido
                result.onSuccess { newComment ->
                    println("Comentário adicionado com sucesso: ${newComment.id}")

                    // Atualizar o cache
                    FeedCache.addComment(voteId, newComment)

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

                // Atualizar o estado local imediatamente para feedback rápido
                result.onSuccess {
                    println("Comentário removido com sucesso")

                    // Atualizar o cache
                    FeedCache.removeComment(commentId)

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

    override fun onCleared() {
        super.onCleared()

        // Cancela todos os jobs ao destruir o ViewModel
        cancelExistingJobs()
        supervisorJob.cancel()
    }
}