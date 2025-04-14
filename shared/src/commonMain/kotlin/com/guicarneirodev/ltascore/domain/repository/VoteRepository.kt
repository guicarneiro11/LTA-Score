package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.Vote
import com.guicarneirodev.ltascore.domain.models.VoteSummary
import kotlinx.coroutines.flow.Flow

interface VoteRepository {
    /**
     * Registra um voto para um jogador em uma partida
     */
    suspend fun submitVote(vote: Vote)

    /**
     * Obtém os votos de um usuário
     */
    suspend fun getUserVotes(userId: String): Flow<List<Vote>>

    /**
     * Obtém o voto de um usuário para um jogador em uma partida específica
     */
    suspend fun getUserVoteForPlayer(userId: String, matchId: String, playerId: String): Flow<Vote?>

    /**
     * Obtém o resumo de votos para todos os jogadores em uma partida
     */
    suspend fun getMatchVoteSummary(matchId: String): Flow<List<VoteSummary>>

    /**
     * Verifica se o usuário já votou em todos os jogadores de uma partida
     */
    suspend fun hasUserVotedForMatch(userId: String, matchId: String): Flow<Boolean>

    /**
     * Obtém o histórico de votos de um usuário
     */
    suspend fun getUserVoteHistory(userId: String): Flow<List<UserVoteHistoryItem>>

    /**
     * Adiciona um voto ao histórico do usuário
     */
    suspend fun addVoteToUserHistory(userId: String, historyItem: UserVoteHistoryItem)
}