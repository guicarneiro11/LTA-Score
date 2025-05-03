package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.MatchPrediction
import com.guicarneirodev.ltascore.domain.models.MatchPredictionStats
import com.guicarneirodev.ltascore.domain.repository.MatchPredictionRepository
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

class ManageMatchPredictionsUseCase(
    private val predictionRepository: MatchPredictionRepository,
    private val userRepository: UserRepository
) {
    suspend fun submitPrediction(matchId: String, teamId: String): Result<Unit> {
        return try {
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            println("Submitting prediction: matchId=$matchId, teamId=$teamId, userId=${currentUser.id}")

            val predictionId = "${currentUser.id}_${matchId}"

            val prediction = MatchPrediction(
                id = predictionId,
                matchId = matchId,
                userId = currentUser.id,
                predictedTeamId = teamId,
                timestamp = Clock.System.now()
            )

            predictionRepository.submitPrediction(prediction)
        } catch (e: Exception) {
            println("Error in ManageMatchPredictionsUseCase: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserPrediction(matchId: String): Flow<MatchPrediction?> {
        val currentUser = userRepository.getCurrentUser().first()
            ?: return kotlinx.coroutines.flow.flowOf(null)

        return predictionRepository.getUserPrediction(currentUser.id, matchId)
    }

    suspend fun getMatchPredictionStats(matchId: String): Flow<MatchPredictionStats> {
        return predictionRepository.getMatchPredictionStats(matchId)
    }

    suspend fun hasUserPredicted(matchId: String): Flow<Boolean> {
        val currentUser = userRepository.getCurrentUser().first()
            ?: return kotlinx.coroutines.flow.flowOf(false)

        return predictionRepository.hasUserPredicted(currentUser.id, matchId)
    }
}