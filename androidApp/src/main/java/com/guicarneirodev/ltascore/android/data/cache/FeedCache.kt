package com.guicarneirodev.ltascore.android.data.cache

import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.VoteComment
import com.guicarneirodev.ltascore.domain.models.VoteReaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cache singleton para dados do feed de amigos
 * Este cache permanece ativo enquanto o app estiver em execução
 */
object FeedCache {
    // Cache para dados do feed
    private val _cachedFeed = MutableStateFlow<List<FriendVoteHistoryItem>>(emptyList())
    val cachedFeed: StateFlow<List<FriendVoteHistoryItem>> = _cachedFeed.asStateFlow()

    // Cache para reações
    private val _cachedReactions = MutableStateFlow<Map<String, List<VoteReaction>>>(emptyMap())
    val cachedReactions: StateFlow<Map<String, List<VoteReaction>>> = _cachedReactions.asStateFlow()

    // Cache para reações do usuário
    private val _cachedUserReactions = MutableStateFlow<Map<String, VoteReaction?>>(emptyMap())
    val cachedUserReactions: StateFlow<Map<String, VoteReaction?>> = _cachedUserReactions.asStateFlow()

    // Cache para comentários
    private val _cachedComments = MutableStateFlow<Map<String, List<VoteComment>>>(emptyMap())
    val cachedComments: StateFlow<Map<String, List<VoteComment>>> = _cachedComments.asStateFlow()

    // Função para atualizar o feed
    fun updateFeed(feed: List<FriendVoteHistoryItem>) {
        _cachedFeed.value = feed
    }

    // Função para atualizar as reações de um voto
    fun updateReactions(voteId: String, reactions: List<VoteReaction>) {
        val currentReactions = _cachedReactions.value.toMutableMap()
        currentReactions[voteId] = reactions
        _cachedReactions.value = currentReactions
    }

    // Função para atualizar a reação do usuário para um voto
    fun updateUserReaction(voteId: String, reaction: VoteReaction?) {
        val currentUserReactions = _cachedUserReactions.value.toMutableMap()
        currentUserReactions[voteId] = reaction
        _cachedUserReactions.value = currentUserReactions
    }

    // Função para atualizar os comentários de um voto
    fun updateComments(voteId: String, comments: List<VoteComment>) {
        val currentComments = _cachedComments.value.toMutableMap()
        currentComments[voteId] = comments
        _cachedComments.value = currentComments
    }

    // Função para adicionar um comentário
    fun addComment(voteId: String, comment: VoteComment) {
        val currentComments = _cachedComments.value.toMutableMap()
        val voteComments = currentComments[voteId]?.toMutableList() ?: mutableListOf()
        voteComments.add(comment)
        currentComments[voteId] = voteComments
        _cachedComments.value = currentComments
    }

    // Função para remover um comentário
    fun removeComment(commentId: String) {
        val updatedComments = _cachedComments.value.mapValues { (_, comments) ->
            comments.filter { it.id != commentId }
        }.toMutableMap()
        _cachedComments.value = updatedComments
    }

    // Função para adicionar ou atualizar uma reação
    fun addOrUpdateReaction(voteId: String, reaction: VoteReaction) {
        // Atualizar a lista de reações
        val currentReactions = _cachedReactions.value.toMutableMap()
        val voteReactions = currentReactions[voteId]?.toMutableList() ?: mutableListOf()

        // Remover reação anterior do mesmo usuário, se existir
        val updatedReactions = voteReactions.filter { it.userId != reaction.userId }.toMutableList()
        updatedReactions.add(reaction)

        currentReactions[voteId] = updatedReactions
        _cachedReactions.value = currentReactions

        // Atualizar a reação do usuário
        if (reaction.userId == reaction.userId) { // Verificação redundante, mas mantida para clareza
            updateUserReaction(voteId, reaction)
        }
    }

    // Função para remover uma reação
    fun removeReaction(voteId: String, userId: String) {
        // Atualizar a lista de reações
        val currentReactions = _cachedReactions.value.toMutableMap()
        val voteReactions = currentReactions[voteId]?.toMutableList() ?: mutableListOf()

        // Filtrar para remover a reação do usuário
        val updatedReactions = voteReactions.filter { it.userId != userId }

        currentReactions[voteId] = updatedReactions
        _cachedReactions.value = currentReactions

        // Remover a reação do usuário
        val currentUserReactions = _cachedUserReactions.value.toMutableMap()
        currentUserReactions.remove(voteId)
        _cachedUserReactions.value = currentUserReactions
    }

    // Função para limpar o cache
    fun clear() {
        _cachedFeed.value = emptyList()
        _cachedReactions.value = emptyMap()
        _cachedUserReactions.value = emptyMap()
        _cachedComments.value = emptyMap()
    }
}