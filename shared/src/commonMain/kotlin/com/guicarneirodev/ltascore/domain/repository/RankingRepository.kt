package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.PlayerRankingItem
import com.guicarneirodev.ltascore.domain.models.TimeFrame
import kotlinx.coroutines.flow.Flow

/**
 * Interface do repositório responsável por buscar dados de ranking dos jogadores
 */
interface RankingRepository {
    /**
     * Obtém o ranking geral de todos os jogadores
     *
     * @param limit Número máximo de jogadores a retornar
     * @return Flow com a lista de jogadores ordenada por avaliação média
     */
    suspend fun getGeneralRanking(limit: Int = 50): Flow<List<PlayerRankingItem>>

    /**
     * Obtém o ranking filtrado por time
     *
     * @param teamId ID do time para filtrar
     * @return Flow com a lista de jogadores do time ordenada por avaliação média
     */
    suspend fun getRankingByTeam(teamId: String): Flow<List<PlayerRankingItem>>

    /**
     * Obtém o ranking filtrado por posição
     *
     * @param position Posição para filtrar
     * @return Flow com a lista de jogadores da posição ordenada por avaliação média
     */
    suspend fun getRankingByPosition(position: PlayerPosition): Flow<List<PlayerRankingItem>>

    /**
     * Obtém o ranking filtrado por período de tempo
     *
     * @param timeFrame Período de tempo para filtrar
     * @return Flow com a lista de jogadores ordenada por avaliação média no período
     */
    suspend fun getRankingByTimeFrame(timeFrame: TimeFrame): Flow<List<PlayerRankingItem>>

    /**
     * Obtém o ranking ordenado por total de votos
     *
     * @param limit Número máximo de jogadores a retornar
     * @return Flow com a lista de jogadores ordenada por número de votos
     */
    suspend fun getMostVotedRanking(limit: Int = 50): Flow<List<PlayerRankingItem>>

    /**
     * Atualiza o cache de ranking
     */
    suspend fun refreshRankingData()
}