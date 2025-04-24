package com.guicarneirodev.ltascore.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.ltascore.android.data.cache.FeedCache
import com.guicarneirodev.ltascore.android.util.StringResources
import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.TeamFeedItem
import com.guicarneirodev.ltascore.domain.models.VoteComment
import com.guicarneirodev.ltascore.domain.models.VoteReaction
import com.guicarneirodev.ltascore.domain.repository.TeamFeedRepository
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
import kotlinx.datetime.Clock
import com.guicarneirodev.ltascore.android.R

data class VoteReactionsState(
    val reactions: List<VoteReaction> = emptyList(),
    val userReaction: VoteReaction? = null
)

data class FriendsFeedUiState(
    val isLoading: Boolean = false,
    val feed: List<FriendVoteHistoryItem> = emptyList(),
    val teamFeed: List<TeamFeedItem> = emptyList(),
    val activeTab: Int = 0,
    val groupedFeed: Map<String, List<FriendVoteHistoryItem>> = emptyMap(),
    val groupedTeamFeed: Map<String, List<TeamFeedItem>> = emptyMap(),
    val voteReactions: Map<String, VoteReactionsState> = emptyMap(),
    val teamVoteReactions: Map<String, VoteReactionsState> = emptyMap(),
    val voteComments: Map<String, List<VoteComment>> = emptyMap(),
    val teamVoteComments: Map<String, List<VoteComment>> = emptyMap(),
    val currentUserId: String = "",
    val currentUserTeamId: String? = null,
    val error: String? = null
)

class FriendsFeedViewModel(
    private val getFriendsFeedUseCase: GetFriendsFeedUseCase,
    private val voteSocialRepository: VoteSocialRepository,
    private val teamFeedRepository: TeamFeedRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        const val CURRENT_USER_KEY = "__CURRENT_USER__"
    }

    private val _uiState = MutableStateFlow(FriendsFeedUiState(isLoading = true))
    val uiState: StateFlow<FriendsFeedUiState> = _uiState.asStateFlow()

    private var reactionsJobs = mutableMapOf<String, Job>()
    private var commentsJobs = mutableMapOf<String, Job>()
    private var teamReactionsJobs = mutableMapOf<String, Job>()
    private var teamCommentsJobs = mutableMapOf<String, Job>()

    private val supervisorJob = SupervisorJob()

    init {
        loadCurrentUser()
        initializeFromCache()
        loadFriendsFeed()
    }

    fun setActiveTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tabIndex)

        if (tabIndex == 0) {
            loadFriendsFeed()
        } else {
            loadTeamFeed()
        }
    }

    private fun initializeFromCache() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser().first()
            if (user != null) {
                _uiState.value = _uiState.value.copy(
                    currentUserId = user.id,
                    currentUserTeamId = user.favoriteTeamId
                )
            }

            combine(
                FeedCache.cachedFeed,
                FeedCache.cachedReactions,
                FeedCache.cachedUserReactions,
                FeedCache.cachedComments
            ) { feed, reactions, userReactions, comments ->
                if (feed.isNotEmpty()) {
                    val groupedItems = feed.groupBy { item ->
                        val prefix = if (item.friendId == _uiState.value.currentUserId) {
                            CURRENT_USER_KEY
                        } else {
                            item.friendUsername
                        }
                        "$prefix: ${item.matchId}|${formatDateForGrouping(item.matchDate)}"
                    }

                    val reactionsState = reactions.mapValues { (voteId, reactionsList) ->
                        VoteReactionsState(
                            reactions = reactionsList,
                            userReaction = userReactions[voteId]
                        )
                    }

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
                        currentUserId = user.id,
                        currentUserTeamId = user.favoriteTeamId
                    )
                    println("Usuário atual carregado: ${user.id}, time: ${user.favoriteTeamId}")
                } else {
                    println("Nenhum usuário logado encontrado")
                }
            } catch (e: Exception) {
                println("Erro ao carregar usuário atual: ${e.message}")
            }
        }
    }

    fun loadFriendsFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                getFriendsFeedUseCase.execute().collect { feedItems ->
                    println("Feed de amigos recebido: ${feedItems.size} itens")

                    FeedCache.updateFeed(feedItems)

                    val groupedItems = feedItems.groupBy { item ->
                        val prefix = if (item.friendId == _uiState.value.currentUserId) {
                            CURRENT_USER_KEY
                        } else {
                            item.friendUsername
                        }
                        "$prefix: ${item.matchId}|${formatDateForGrouping(item.matchDate)}"
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        feed = feedItems,
                        groupedFeed = groupedItems
                    )

                    cancelExistingJobs()

                    feedItems.forEach { voteItem ->
                        println("Carregando reações e comentários para voto: ${voteItem.id}")
                        loadReactionsForVote(voteItem.id)
                        loadCommentsForVote(voteItem.id)
                    }
                }
            } catch (e: Exception) {
                println("Erro ao carregar feed de amigos: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.feed_load_error, e.message ?: "")
                )
            }
        }
    }

    fun loadTeamFeed() {
        viewModelScope.launch {
            val teamId = _uiState.value.currentUserTeamId

            if (teamId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getString(R.string.select_team_message)
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                teamFeedRepository.getTeamFeed(teamId).collect { feedItems ->
                    println("Feed da torcida recebido: ${feedItems.size} itens")

                    val groupedItems = feedItems.groupBy { item ->
                        "${item.username}: ${item.matchId}|${formatDateForGrouping(item.matchDate)}"
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        teamFeed = feedItems,
                        groupedTeamFeed = groupedItems
                    )

                    cancelTeamFeedJobs()

                    feedItems.forEach { voteItem ->
                        println("Carregando reações e comentários para voto da torcida: ${voteItem.id}")
                        loadReactionsForTeamVote(voteItem.id)
                        loadCommentsForTeamVote(voteItem.id)
                    }
                }
            } catch (e: Exception) {
                println("Erro ao carregar feed da torcida: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = StringResources.getStringFormatted(R.string.team_feed_load_error, e.message ?: "")
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

    private fun cancelTeamFeedJobs() {
        teamReactionsJobs.forEach { (_, job) -> job.cancel() }
        teamCommentsJobs.forEach { (_, job) -> job.cancel() }
        teamReactionsJobs.clear()
        teamCommentsJobs.clear()
    }

    private fun loadReactionsForVote(voteId: String) {
        reactionsJobs[voteId] = viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            try {
                println("Iniciando carregamento de reações para: $voteId")

                voteSocialRepository.getReactionsForVote(voteId).collect { reactions ->
                    println("Reações recebidas para $voteId: ${reactions.size}")

                    FeedCache.updateReactions(voteId, reactions)

                    val userReaction = withContext(Dispatchers.IO) {
                        voteSocialRepository.getUserReactionForVote(voteId).first()
                    }

                    FeedCache.updateUserReaction(voteId, userReaction)

                    println("Reação do usuário atual para $voteId: ${userReaction?.reaction ?: "nenhuma"}")

                    withContext(Dispatchers.Main) {
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
                e.printStackTrace()
            }
        }
    }

    private fun loadCommentsForVote(voteId: String) {
        commentsJobs[voteId] = viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            try {
                println("Iniciando carregamento de comentários para: $voteId")

                voteSocialRepository.getCommentsForVote(voteId).collect { comments ->
                    println("Comentários recebidos para $voteId: ${comments.size}")

                    FeedCache.updateComments(voteId, comments)

                    withContext(Dispatchers.Main) {
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

    private fun loadReactionsForTeamVote(voteId: String) {
        teamReactionsJobs[voteId] = viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            try {
                println("Iniciando carregamento de reações para voto da torcida: $voteId")

                teamFeedRepository.getReactionsForTeamVote(voteId).collect { reactions ->
                    println("Reações recebidas para voto da torcida $voteId: ${reactions.size}")

                    val currentUser = withContext(Dispatchers.IO) {
                        userRepository.getCurrentUser().first()
                    }

                    val userReaction = reactions.find { it.userId == currentUser?.id }

                    withContext(Dispatchers.Main) {
                        val currentReactions = _uiState.value.teamVoteReactions
                        val updatedReactions = currentReactions.toMutableMap().apply {
                            this[voteId] = VoteReactionsState(
                                reactions = reactions,
                                userReaction = userReaction
                            )
                        }

                        _uiState.value = _uiState.value.copy(
                            teamVoteReactions = updatedReactions
                        )
                    }
                }
            } catch (e: Exception) {
                println("Erro ao carregar reações para voto da torcida $voteId: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun loadCommentsForTeamVote(voteId: String) {
        teamCommentsJobs[voteId] = viewModelScope.launch(Dispatchers.IO + supervisorJob) {
            try {
                println("Iniciando carregamento de comentários para voto da torcida: $voteId")

                teamFeedRepository.getCommentsForTeamVote(voteId).collect { comments ->
                    println("Comentários recebidos para voto da torcida $voteId: ${comments.size}")

                    withContext(Dispatchers.Main) {
                        val currentComments = _uiState.value.teamVoteComments
                        val updatedComments = currentComments.toMutableMap().apply {
                            this[voteId] = comments
                        }

                        _uiState.value = _uiState.value.copy(
                            teamVoteComments = updatedComments
                        )
                    }
                }
            } catch (e: Exception) {
                println("Erro ao carregar comentários para voto da torcida $voteId: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun addReaction(voteId: String, reaction: String) {
        viewModelScope.launch {
            try {
                println("Adicionando reação '$reaction' ao voto $voteId")

                val result = voteSocialRepository.addReaction(voteId, reaction)

                result.onSuccess { newReaction ->
                    println("Reação adicionada com sucesso: ${newReaction.reaction}")

                    FeedCache.addOrUpdateReaction(voteId, newReaction)

                    val currentState = _uiState.value
                    val currentReactionsState = currentState.voteReactions[voteId] ?: VoteReactionsState()

                    val filteredReactions = currentReactionsState.reactions.filter {
                        it.userId != newReaction.userId
                    }

                    val updatedReactions = filteredReactions + newReaction

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
                    _uiState.value = _uiState.value.copy(
                        error = StringResources.getStringFormatted(R.string.reaction_add_error, error.message ?: "")
                    )
                }
            } catch (e: Exception) {
                println("Erro ao adicionar reação: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    error = StringResources.getStringFormatted(R.string.reaction_add_error, e.message ?: "")
                )
            }
        }
    }

    fun removeReaction(voteId: String) {
        viewModelScope.launch {
            try {
                println("Removendo reação do voto $voteId")

                val currentUserId = _uiState.value.currentUserId

                val currentState = _uiState.value
                val currentReactionsState = currentState.voteReactions[voteId] ?: VoteReactionsState()
                val userReaction = currentReactionsState.userReaction

                val result = voteSocialRepository.removeReaction(voteId)

                result.onSuccess {
                    println("Reação removida com sucesso")

                    FeedCache.removeReaction(voteId, currentUserId)

                    val userId = userReaction?.userId ?: currentState.currentUserId
                    val updatedReactions = currentReactionsState.reactions.filter {
                        it.userId != userId
                    }

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

                val currentState = _uiState.value
                val existingComments = currentState.voteComments[voteId] ?: emptyList()

                val recentDuplicateExists = existingComments.any { existingComment ->
                    val now = Clock.System.now()
                    val timeDiff = now - existingComment.timestamp
                    existingComment.text == text &&
                            existingComment.userId == currentState.currentUserId &&
                            timeDiff.inWholeSeconds < 5
                }

                if (recentDuplicateExists) {
                    println("Ignorando comentário duplicado")
                    return@launch
                }

                val result = voteSocialRepository.addComment(voteId, text)

                result.onSuccess { newComment ->
                    println("Comentário adicionado com sucesso: ${newComment.id}")

                    FeedCache.addComment(voteId, newComment)

                    val updatedComments = existingComments + newComment

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

                val result = voteSocialRepository.removeComment(commentId)

                result.onSuccess {
                    println("Comentário removido com sucesso")

                    FeedCache.removeComment(commentId)

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

    fun addReactionToTeamVote(voteId: String, reaction: String) {
        viewModelScope.launch {
            try {
                println("Adicionando reação '$reaction' ao voto da torcida $voteId")

                val result = teamFeedRepository.addReactionToTeamVote(voteId, reaction)

                result.onSuccess { newReaction ->
                    println("Reação adicionada com sucesso ao voto da torcida: ${newReaction.reaction}")

                    val currentState = _uiState.value
                    val currentReactionsState = currentState.teamVoteReactions[voteId] ?: VoteReactionsState()

                    val filteredReactions = currentReactionsState.reactions.filter {
                        it.userId != newReaction.userId
                    }

                    val updatedReactions = filteredReactions + newReaction

                    val updatedReactionsMap = currentState.teamVoteReactions.toMutableMap().apply {
                        this[voteId] = VoteReactionsState(
                            reactions = updatedReactions,
                            userReaction = newReaction
                        )
                    }

                    _uiState.value = currentState.copy(
                        teamVoteReactions = updatedReactionsMap
                    )
                }.onFailure { error ->
                    println("Erro ao adicionar reação ao voto da torcida: ${error.message}")
                }
            } catch (e: Exception) {
                println("Erro ao adicionar reação ao voto da torcida: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun removeReactionFromTeamVote(voteId: String) {
        viewModelScope.launch {
            try {
                println("Removendo reação do voto da torcida $voteId")

                val currentUserId = _uiState.value.currentUserId

                val currentState = _uiState.value
                val currentReactionsState = currentState.teamVoteReactions[voteId] ?: VoteReactionsState()
                val userReaction = currentReactionsState.userReaction

                val result = teamFeedRepository.removeReactionFromTeamVote(voteId)

                result.onSuccess {
                    println("Reação removida com sucesso do voto da torcida")

                    val userId = userReaction?.userId ?: currentState.currentUserId
                    val updatedReactions = currentReactionsState.reactions.filter {
                        it.userId != userId
                    }

                    val updatedReactionsMap = currentState.teamVoteReactions.toMutableMap().apply {
                        this[voteId] = VoteReactionsState(
                            reactions = updatedReactions,
                            userReaction = null
                        )
                    }

                    _uiState.value = currentState.copy(
                        teamVoteReactions = updatedReactionsMap
                    )
                }.onFailure { error ->
                    println("Erro ao remover reação do voto da torcida: ${error.message}")
                }
            } catch (e: Exception) {
                println("Erro ao remover reação do voto da torcida: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun addCommentToTeamVote(voteId: String, text: String) {
        viewModelScope.launch {
            try {
                println("Adicionando comentário ao voto da torcida $voteId: '$text'")

                val currentState = _uiState.value
                val existingComments = currentState.teamVoteComments[voteId] ?: emptyList()

                val recentDuplicateExists = existingComments.any { existingComment ->
                    val now = Clock.System.now()
                    val timeDiff = now - existingComment.timestamp
                    existingComment.text == text &&
                            existingComment.userId == currentState.currentUserId &&
                            timeDiff.inWholeSeconds < 5
                }

                if (recentDuplicateExists) {
                    println("Ignorando comentário duplicado no voto da torcida")
                    return@launch
                }

                val result = teamFeedRepository.addCommentToTeamVote(voteId, text)

                result.onSuccess { newComment ->
                    println("Comentário adicionado com sucesso ao voto da torcida: ${newComment.id}")

                    val updatedComments = existingComments + newComment

                    val updatedCommentsMap = currentState.teamVoteComments.toMutableMap().apply {
                        this[voteId] = updatedComments
                    }

                    _uiState.value = currentState.copy(
                        teamVoteComments = updatedCommentsMap
                    )
                }.onFailure { error ->
                    println("Erro ao adicionar comentário ao voto da torcida: ${error.message}")
                }
            } catch (e: Exception) {
                println("Erro ao adicionar comentário ao voto da torcida: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun deleteCommentFromTeamVote(commentId: String) {
        viewModelScope.launch {
            try {
                println("Removendo comentário $commentId do voto da torcida")

                val result = teamFeedRepository.removeCommentFromTeamVote(commentId)

                result.onSuccess {
                    println("Comentário removido com sucesso do voto da torcida")

                    val currentState = _uiState.value
                    val updatedCommentsMap = currentState.teamVoteComments.mapValues { (_, comments) ->
                        comments.filter { it.id != commentId }
                    }.toMutableMap()

                    _uiState.value = currentState.copy(
                        teamVoteComments = updatedCommentsMap
                    )
                }.onFailure { error ->
                    println("Erro ao remover comentário do voto da torcida: ${error.message}")
                }
            } catch (e: Exception) {
                println("Erro ao remover comentário do voto da torcida: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun formatDateForGrouping(date: Instant): String {
        val localDate = date.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${localDate.dayOfMonth}/${localDate.monthNumber}/${localDate.year}"
    }

    override fun onCleared() {
        super.onCleared()
        cancelExistingJobs()
        cancelTeamFeedJobs()
        supervisorJob.cancel()
    }
}