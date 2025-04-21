package com.guicarneirodev.ltascore.android.data.cache

import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.VoteComment
import com.guicarneirodev.ltascore.domain.models.VoteReaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FeedCache {
    private val _cachedFeed = MutableStateFlow<List<FriendVoteHistoryItem>>(emptyList())
    val cachedFeed: StateFlow<List<FriendVoteHistoryItem>> = _cachedFeed.asStateFlow()

    private val _cachedReactions = MutableStateFlow<Map<String, List<VoteReaction>>>(emptyMap())
    val cachedReactions: StateFlow<Map<String, List<VoteReaction>>> = _cachedReactions.asStateFlow()

    private val _cachedUserReactions = MutableStateFlow<Map<String, VoteReaction>>(emptyMap())
    val cachedUserReactions: StateFlow<Map<String, VoteReaction>> = _cachedUserReactions.asStateFlow()

    private val _cachedComments = MutableStateFlow<Map<String, List<VoteComment>>>(emptyMap())
    val cachedComments: StateFlow<Map<String, List<VoteComment>>> = _cachedComments.asStateFlow()

    fun updateFeed(feed: List<FriendVoteHistoryItem>) {
        _cachedFeed.value = feed
    }

    fun updateReactions(voteId: String, reactions: List<VoteReaction>) {
        val current = _cachedReactions.value.toMutableMap()
        current[voteId] = reactions
        _cachedReactions.value = current
    }

    fun updateUserReaction(voteId: String, reaction: VoteReaction?) {
        val current = _cachedUserReactions.value.toMutableMap()
        if (reaction != null) {
            current[voteId] = reaction
        } else {
            current.remove(voteId)
        }
        _cachedUserReactions.value = current
    }

    fun addOrUpdateReaction(voteId: String, reaction: VoteReaction) {
        updateUserReaction(voteId, reaction)

        val currentReactions = _cachedReactions.value[voteId] ?: emptyList()

        val filteredReactions = currentReactions.filter { it.userId != reaction.userId }

        val updatedReactions = filteredReactions + reaction

        updateReactions(voteId, updatedReactions)
    }

    fun removeReaction(voteId: String, userId: String) {
        val current = _cachedUserReactions.value.toMutableMap()
        current.remove(voteId)
        _cachedUserReactions.value = current
        val currentReactions = _cachedReactions.value[voteId] ?: emptyList()
        val updatedReactions = currentReactions.filter { it.userId != userId }
        updateReactions(voteId, updatedReactions)
    }

    fun updateComments(voteId: String, comments: List<VoteComment>) {
        val current = _cachedComments.value.toMutableMap()

        val uniqueComments = comments.distinctBy { it.id }

        current[voteId] = uniqueComments
        _cachedComments.value = current
    }

    fun addComment(voteId: String, comment: VoteComment) {
        val currentComments = _cachedComments.value[voteId] ?: emptyList()

        if (currentComments.none { it.id == comment.id }) {
            val updatedComments = currentComments + comment
            updateComments(voteId, updatedComments)
        }
    }

    fun removeComment(commentId: String) {
        val currentCommentsMap = _cachedComments.value.toMutableMap()

        currentCommentsMap.forEach { (voteId, comments) ->
            if (comments.any { it.id == commentId }) {
                val updatedComments = comments.filter { it.id != commentId }
                currentCommentsMap[voteId] = updatedComments
            }
        }

        _cachedComments.value = currentCommentsMap
    }
}