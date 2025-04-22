package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.TeamFeedItem
import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.VoteComment
import com.guicarneirodev.ltascore.domain.models.VoteReaction
import kotlinx.coroutines.flow.Flow

interface TeamFeedRepository {
    fun getTeamFeed(teamId: String): Flow<List<TeamFeedItem>>
    suspend fun shareVoteToTeamFeed(userId: String, teamId: String, vote: UserVoteHistoryItem): Result<Unit>
    suspend fun getReactionsForTeamVote(voteId: String): Flow<List<VoteReaction>>
    suspend fun getCommentsForTeamVote(voteId: String): Flow<List<VoteComment>>
    suspend fun addReactionToTeamVote(voteId: String, reaction: String): Result<VoteReaction>
    suspend fun removeReactionFromTeamVote(voteId: String): Result<Unit>
    suspend fun addCommentToTeamVote(voteId: String, text: String): Result<VoteComment>
    suspend fun removeCommentFromTeamVote(commentId: String): Result<Unit>
}