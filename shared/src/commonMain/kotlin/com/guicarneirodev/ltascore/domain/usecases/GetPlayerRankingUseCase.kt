package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.PlayerRankingItem
import com.guicarneirodev.ltascore.domain.models.RankingFilter
import com.guicarneirodev.ltascore.domain.models.RankingFilterState
import com.guicarneirodev.ltascore.domain.models.TimeFrame
import com.guicarneirodev.ltascore.domain.repository.RankingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class GetPlayerRankingUseCase(
    private val rankingRepository: RankingRepository
) {
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

    suspend fun refreshRanking() {
        rankingRepository.refreshRankingData()
    }
}