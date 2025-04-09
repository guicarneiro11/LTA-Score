package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.MatchState
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    /**
     * Obtém todas as partidas da LTA Sul (ou Norte)
     */
    suspend fun getMatches(leagueSlug: String): Flow<List<Match>>

    /**
     * Obtém partidas filtradas por estado
     */
    suspend fun getMatchesByState(leagueSlug: String, state: MatchState): Flow<List<Match>>

    /**
     * Obtém uma partida específica por ID
     */
    suspend fun getMatchById(matchId: String): Flow<Match?>

    /**
     * Obtém partidas por semana/bloco
     */
    suspend fun getMatchesByBlock(leagueSlug: String, blockName: String): Flow<List<Match>>

    /**
     * Atualiza o cache de partidas
     */
    suspend fun refreshMatches(leagueSlug: String)
}