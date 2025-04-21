package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.MatchState
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    suspend fun getMatches(leagueSlug: String): Flow<List<Match>>

    suspend fun getMatchesByState(leagueSlug: String, state: MatchState): Flow<List<Match>>

    suspend fun getMatchById(matchId: String): Flow<Match?>

    suspend fun getMatchesByBlock(leagueSlug: String, blockName: String): Flow<List<Match>>

    suspend fun refreshMatches(leagueSlug: String)
}