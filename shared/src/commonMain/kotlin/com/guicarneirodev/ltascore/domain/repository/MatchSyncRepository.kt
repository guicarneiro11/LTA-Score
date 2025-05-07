package com.guicarneirodev.ltascore.domain.repository

interface MatchSyncRepository {
    suspend fun syncMatchesToFirestore()
    suspend fun syncLiveMatches()
    suspend fun forceUpdateMatchState(matchId: String, newState: String)
}