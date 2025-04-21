package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.PlayerRankingItem
import com.guicarneirodev.ltascore.domain.models.TimeFrame
import kotlinx.coroutines.flow.Flow

interface RankingRepository {

    suspend fun getGeneralRanking(limit: Int = 50): Flow<List<PlayerRankingItem>>

    suspend fun getRankingByTeam(teamId: String): Flow<List<PlayerRankingItem>>

    suspend fun getRankingByPosition(position: PlayerPosition): Flow<List<PlayerRankingItem>>

    suspend fun getRankingByTimeFrame(timeFrame: TimeFrame): Flow<List<PlayerRankingItem>>

    suspend fun getMostVotedRanking(limit: Int = 50): Flow<List<PlayerRankingItem>>

    suspend fun refreshRankingData()
}