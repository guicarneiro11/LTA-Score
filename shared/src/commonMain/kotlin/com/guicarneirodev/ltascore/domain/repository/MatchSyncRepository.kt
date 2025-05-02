package com.guicarneirodev.ltascore.domain.repository

interface MatchSyncRepository {
    suspend fun syncMatchesToFirestore()
}