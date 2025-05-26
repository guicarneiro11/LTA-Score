package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.ltascore.domain.models.MatchPrediction
import com.guicarneirodev.ltascore.domain.models.MatchPredictionStats
import com.guicarneirodev.ltascore.domain.repository.MatchPredictionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.Date

class FirebaseMatchPredictionRepository(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MatchPredictionRepository {

    private val predictionsCollection = firestore.collection("match_predictions")

    override suspend fun submitPrediction(prediction: MatchPrediction): Result<Unit> {
        return try {
            val existingPredictions = predictionsCollection
                .whereEqualTo("userId", prediction.userId)
                .whereEqualTo("matchId", prediction.matchId)
                .get()
                .await()

            if (!existingPredictions.isEmpty) {
                val existingDoc = existingPredictions.documents.first()
                val existingTeamId = existingDoc.getString("predictedTeamId")

                if (existingTeamId == prediction.predictedTeamId) {
                    predictionsCollection.document(existingDoc.id)
                        .delete()
                        .await()

                    return Result.success(Unit)
                } else {
                    println("User changing vote from $existingTeamId to ${prediction.predictedTeamId}")
                    predictionsCollection.document(existingDoc.id)
                        .delete()
                        .await()
                }
            }

            val predictionId = "${prediction.userId}_${prediction.matchId}"
            val predictionData = hashMapOf<String, Any>(
                "id" to predictionId,
                "matchId" to prediction.matchId,
                "userId" to prediction.userId,
                "predictedTeamId" to prediction.predictedTeamId,
                "timestamp" to Date.from(prediction.timestamp.toJavaInstant())
            )

            predictionsCollection.document(predictionId)
                .set(predictionData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            println("Error in submitPrediction: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getUserPrediction(userId: String, matchId: String): Flow<MatchPrediction?> = callbackFlow {
        val listener = predictionsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("matchId", matchId)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val doc = snapshot.documents.first()
                try {
                    val id = doc.getString("id") ?: return@addSnapshotListener
                    val predictedTeamId = doc.getString("predictedTeamId") ?: return@addSnapshotListener
                    val timestampDate = doc.getDate("timestamp")

                    val timestamp = if (timestampDate != null) {
                        Instant.fromEpochMilliseconds(timestampDate.time)
                    } else {
                        Clock.System.now()
                    }

                    val prediction = MatchPrediction(
                        id = id,
                        matchId = matchId,
                        userId = userId,
                        predictedTeamId = predictedTeamId,
                        timestamp = timestamp
                    )

                    trySend(prediction)
                } catch (_: Exception) {
                    trySend(null)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun getMatchPredictionStats(matchId: String): Flow<MatchPredictionStats> = callbackFlow {
        val listener = predictionsCollection
            .whereEqualTo("matchId", matchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(MatchPredictionStats(matchId, 0, emptyMap(), emptyMap()))
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    trySend(MatchPredictionStats(matchId, 0, emptyMap(), emptyMap()))
                    return@addSnapshotListener
                }

                try {
                    val predictions = snapshot.documents.mapNotNull { doc ->
                        doc.getString("predictedTeamId")
                    }

                    val totalVotes = predictions.size
                    val teamVotes = predictions.groupingBy { it }.eachCount()
                    val percentages = teamVotes.mapValues { (_, votes) ->
                        if (totalVotes > 0) (votes * 100) / totalVotes else 0
                    }

                    val stats = MatchPredictionStats(
                        matchId = matchId,
                        totalVotes = totalVotes,
                        teamVotes = teamVotes,
                        percentages = percentages
                    )

                    trySend(stats)
                } catch (_: Exception) {
                    trySend(MatchPredictionStats(matchId, 0, emptyMap(), emptyMap()))
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun hasUserPredicted(userId: String, matchId: String): Flow<Boolean> = flow {
        try {
            val query = predictionsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("matchId", matchId)
                .limit(1)
                .get()
                .await()

            emit(!query.isEmpty)
        } catch (_: Exception) {
            emit(false)
        }
    }
}