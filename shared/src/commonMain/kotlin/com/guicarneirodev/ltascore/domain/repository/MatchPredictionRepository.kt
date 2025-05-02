package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.MatchPrediction
import com.guicarneirodev.ltascore.domain.models.MatchPredictionStats
import kotlinx.coroutines.flow.Flow

interface MatchPredictionRepository {
    suspend fun submitPrediction(prediction: MatchPrediction): Result<Unit>
    suspend fun getUserPrediction(userId: String, matchId: String): Flow<MatchPrediction?>
    suspend fun getMatchPredictionStats(matchId: String): Flow<MatchPredictionStats>
    suspend fun hasUserPredicted(userId: String, matchId: String): Flow<Boolean>
}