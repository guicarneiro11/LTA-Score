package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.MatchPrediction
import com.guicarneirodev.ltascore.domain.models.MatchPredictionStats
import com.guicarneirodev.ltascore.domain.repository.MatchPredictionRepository
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlin.random.Random

private fun generateUUID(): String {
    val bytes = List(16) { Random.nextInt(0, 256).toByte() }
    val hexChars = "0123456789abcdef"

    return bytes.joinToString("") { byte ->
        val i = byte.toInt() and 0xFF
        val hi = hexChars[i ushr 4]
        val lo = hexChars[i and 0x0F]
        "$hi$lo"
    }
}

class ManageMatchPredictionsUseCase(
    private val predictionRepository: MatchPredictionRepository,
    private val userRepository: UserRepository
) {
    suspend fun submitPrediction(matchId: String, teamId: String): Result<Unit> {
        return try {
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            val prediction = MatchPrediction(
                id = generateUUID(),
                matchId = matchId,
                userId = currentUser.id,
                predictedTeamId = teamId,
                timestamp = Clock.System.now()
            )

            predictionRepository.submitPrediction(prediction)
        } catch (e: Exception) {
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