package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.VoteComment
import com.guicarneirodev.ltascore.domain.models.VoteReaction
import kotlinx.coroutines.flow.Flow

interface VoteSocialRepository {
    // Reações
    suspend fun addReaction(voteId: String, reaction: String): Result<VoteReaction>
    suspend fun removeReaction(voteId: String): Result<Unit>
    fun getReactionsForVote(voteId: String): Flow<List<VoteReaction>>
    fun getUserReactionForVote(voteId: String): Flow<VoteReaction?>

    // Comentários
    suspend fun addComment(voteId: String, text: String): Result<VoteComment>
    suspend fun removeComment(commentId: String): Result<Unit>
    fun getCommentsForVote(voteId: String): Flow<List<VoteComment>>
}