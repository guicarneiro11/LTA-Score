package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.Vote
import com.guicarneirodev.ltascore.domain.models.VoteSummary
import kotlinx.coroutines.flow.Flow

interface VoteRepository {
    suspend fun submitVote(vote: Vote)

    suspend fun getUserVotes(userId: String): Flow<List<Vote>>

    suspend fun getUserVoteForPlayer(userId: String, matchId: String, playerId: String): Flow<Vote?>

    suspend fun getMatchVoteSummary(matchId: String): Flow<List<VoteSummary>>

    suspend fun hasUserVotedForMatch(userId: String, matchId: String): Flow<Boolean>

    suspend fun getUserVoteHistory(userId: String): Flow<List<UserVoteHistoryItem>>

    suspend fun addVoteToUserHistory(userId: String, historyItem: UserVoteHistoryItem)
}