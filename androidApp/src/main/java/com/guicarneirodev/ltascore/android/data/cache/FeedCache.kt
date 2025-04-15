package com.guicarneirodev.ltascore.android.data.cache

import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.VoteComment
import com.guicarneirodev.ltascore.domain.models.VoteReaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cache para o Feed de Amigos
 * Mantém dados em memória entre instâncias do ViewModel
 */
object FeedCache {
    // Feed de amigos
    private val _cachedFeed = MutableStateFlow<List<FriendVoteHistoryItem>>(emptyList())
    val cachedFeed: StateFlow<List<FriendVoteHistoryItem>> = _cachedFeed.asStateFlow()

    // Reações para cada voto
    private val _cachedReactions = MutableStateFlow<Map<String, List<VoteReaction>>>(emptyMap())
    val cachedReactions: StateFlow<Map<String, List<VoteReaction>>> = _cachedReactions.asStateFlow()

    // Reação do usuário atual para cada voto
    private val _cachedUserReactions = MutableStateFlow<Map<String, VoteReaction>>(emptyMap())
    val cachedUserReactions: StateFlow<Map<String, VoteReaction>> = _cachedUserReactions.asStateFlow()

    // Comentários para cada voto
    private val _cachedComments = MutableStateFlow<Map<String, List<VoteComment>>>(emptyMap())
    val cachedComments: StateFlow<Map<String, List<VoteComment>>> = _cachedComments.asStateFlow()

    /**
     * Atualiza o feed armazenado
     */
    fun updateFeed(feed: List<FriendVoteHistoryItem>) {
        _cachedFeed.value = feed
    }

    /**
     * Atualiza as reações para um voto específico
     */
    fun updateReactions(voteId: String, reactions: List<VoteReaction>) {
        val current = _cachedReactions.value.toMutableMap()
        current[voteId] = reactions
        _cachedReactions.value = current
    }

    /**
     * Atualiza a reação do usuário atual para um voto específico
     */
    fun updateUserReaction(voteId: String, reaction: VoteReaction?) {
        val current = _cachedUserReactions.value.toMutableMap()
        if (reaction != null) {
            current[voteId] = reaction
        } else {
            current.remove(voteId)
        }
        _cachedUserReactions.value = current
    }

    /**
     * Adiciona ou atualiza uma reação
     */
    fun addOrUpdateReaction(voteId: String, reaction: VoteReaction) {
        // Atualizar a reação do usuário
        updateUserReaction(voteId, reaction)

        // Atualizar a lista de reações para este voto
        val currentReactions = _cachedReactions.value[voteId] ?: emptyList()

        // Remover reação existente do mesmo usuário (se houver)
        val filteredReactions = currentReactions.filter { it.userId != reaction.userId }

        // Adicionar a nova reação
        val updatedReactions = filteredReactions + reaction

        // Atualizar o cache
        updateReactions(voteId, updatedReactions)
    }

    /**
     * Remove uma reação
     */
    fun removeReaction(voteId: String, userId: String) {
        // Remover a reação do usuário
        val current = _cachedUserReactions.value.toMutableMap()
        current.remove(voteId)
        _cachedUserReactions.value = current

        // Remover da lista de reações
        val currentReactions = _cachedReactions.value[voteId] ?: emptyList()
        val updatedReactions = currentReactions.filter { it.userId != userId }
        updateReactions(voteId, updatedReactions)
    }

    /**
     * Atualiza os comentários para um voto específico
     */
    fun updateComments(voteId: String, comments: List<VoteComment>) {
        val current = _cachedComments.value.toMutableMap()

        // Garantir que não existem comentários duplicados antes de salvar
        val uniqueComments = comments.distinctBy { it.id }

        current[voteId] = uniqueComments
        _cachedComments.value = current
    }

    /**
     * Adiciona um comentário
     */
    fun addComment(voteId: String, comment: VoteComment) {
        val currentComments = _cachedComments.value[voteId] ?: emptyList()

        // Garantir que o comentário não é duplicado
        if (currentComments.none { it.id == comment.id }) {
            val updatedComments = currentComments + comment
            updateComments(voteId, updatedComments)
        }
    }

    /**
     * Remove um comentário
     */
    fun removeComment(commentId: String) {
        val currentCommentsMap = _cachedComments.value.toMutableMap()

        // Para cada voto, verificar se existe o comentário e removê-lo
        currentCommentsMap.forEach { (voteId, comments) ->
            if (comments.any { it.id == commentId }) {
                val updatedComments = comments.filter { it.id != commentId }
                currentCommentsMap[voteId] = updatedComments
            }
        }

        _cachedComments.value = currentCommentsMap
    }

    /**
     * Limpa todos os dados do cache
     */
    fun clearAll() {
        _cachedFeed.value = emptyList()
        _cachedReactions.value = emptyMap()
        _cachedUserReactions.value = emptyMap()
        _cachedComments.value = emptyMap()
    }
}