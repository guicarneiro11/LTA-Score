package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.PlayerRankingItem
import com.guicarneirodev.ltascore.domain.models.RankingFilter
import com.guicarneirodev.ltascore.domain.models.RankingFilterState
import com.guicarneirodev.ltascore.domain.models.TimeFrame
import com.guicarneirodev.ltascore.domain.repository.RankingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Caso de uso para obter dados de ranking dos jogadores
 */
class GetPlayerRankingUseCase(
    private val rankingRepository: RankingRepository
) {
    /**
     * Obtém o ranking de jogadores com base nos filtros aplicados
     *
     * @param filterState Estado atual do filtro
     * @param limit Número máximo de jogadores a retornar
     * @return Flow com a lista de jogadores de acordo com o filtro
     */
    suspend operator fun invoke(
        filterState: RankingFilterState,
        limit: Int = 50
    ): Flow<List<PlayerRankingItem>> {
        return when (filterState.currentFilter) {
            RankingFilter.ALL -> rankingRepository.getGeneralRanking(limit)
            RankingFilter.BY_TEAM -> {
                filterState.selectedTeamId?.let { teamId ->
                    rankingRepository.getRankingByTeam(teamId)
                } ?: rankingRepository.getGeneralRanking(limit)
            }
            RankingFilter.BY_POSITION -> {
                filterState.selectedPosition?.let { position ->
                    rankingRepository.getRankingByPosition(position)
                } ?: rankingRepository.getGeneralRanking(limit)
            }
            RankingFilter.BY_WEEK -> rankingRepository.getRankingByTimeFrame(TimeFrame.CURRENT_WEEK)
            RankingFilter.BY_MONTH -> rankingRepository.getRankingByTimeFrame(TimeFrame.CURRENT_MONTH)
            RankingFilter.TOP_RATED -> rankingRepository.getGeneralRanking(limit)
            RankingFilter.MOST_VOTED -> rankingRepository.getMostVotedRanking(limit)
        }.catch { exception ->
            println("Erro ao obter ranking: ${exception.message}")
            emit(emptyList())
        }
    }

    /**
     * Atualiza o cache de dados de ranking
     */
    suspend fun refreshRanking() {
        rankingRepository.refreshRankingData()
    }
}