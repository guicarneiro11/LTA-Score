package com.guicarneirodev.ltascore.domain.repository

import kotlinx.coroutines.flow.Flow

interface MatchPlayersRepository {
    suspend fun getParticipatingPlayers(matchId: String): Flow<List<String>>
    suspend fun setParticipatingPlayers(matchId: String, playerIds: List<String>): Result<Unit>
}