package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.guicarneirodev.ltascore.domain.models.Vote
import com.guicarneirodev.ltascore.domain.models.VoteSummary
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.Date

class FirebaseVoteRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : VoteRepository {

    private val votesCollection = firestore.collection("votes")
    private val voteSummariesCollection = firestore.collection("vote_summaries")

    override suspend fun submitVote(vote: Vote) {
        try {
            // Referência para o documento do voto
            val voteRef = votesCollection
                .document(vote.matchId)
                .collection("players")
                .document(vote.playerId)
                .collection("user_votes")
                .document(vote.userId)

            // Salvar o voto
            val voteData = hashMapOf(
                "matchId" to vote.matchId,
                "playerId" to vote.playerId,
                "userId" to vote.userId,
                "rating" to vote.rating,
                "timestamp" to Date.from(vote.timestamp.toJavaInstant())
            )

            voteRef.set(voteData).await()

            // Atualizar o resumo de votos
            updateVoteSummary(vote.matchId, vote.playerId)
        } catch (e: Exception) {
            throw Exception("Erro ao enviar voto: ${e.message}")
        }
    }

    override suspend fun getUserVotes(userId: String): Flow<List<Vote>> = callbackFlow {
        // Buscar todos os votos do usuário
        val listener = firestore.collectionGroup("user_votes")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val votes = snapshot.documents.mapNotNull { doc ->
                        try {
                            val matchId = doc.getString("matchId") ?: return@mapNotNull null
                            val playerId = doc.getString("playerId") ?: return@mapNotNull null
                            val rating = doc.getDouble("rating")?.toFloat() ?: return@mapNotNull null
                            val timestamp = doc.getDate("timestamp")?.toInstant() ?: return@mapNotNull null

                            Vote(
                                id = doc.id,
                                matchId = matchId,
                                playerId = playerId,
                                userId = userId,
                                rating = rating,
                                timestamp = Instant.fromEpochMilliseconds(timestamp.toEpochMilli())
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    trySend(votes)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun getUserVoteForPlayer(userId: String, matchId: String, playerId: String): Flow<Vote?> = callbackFlow {
        // Buscar o voto específico
        val listener = votesCollection
            .document(matchId)
            .collection("players")
            .document(playerId)
            .collection("user_votes")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val rating = snapshot.getDouble("rating")?.toFloat() ?: 0f
                        val timestamp = snapshot.getDate("timestamp")?.toInstant() ?: Clock.System.now().toJavaInstant()

                        val vote = Vote(
                            id = snapshot.id,
                            matchId = matchId,
                            playerId = playerId,
                            userId = userId,
                            rating = rating,
                            timestamp = Instant.fromEpochMilliseconds(timestamp.toEpochMilli())
                        )

                        trySend(vote)
                    } catch (e: Exception) {
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun getMatchVoteSummary(matchId: String): Flow<List<VoteSummary>> = callbackFlow {
        // Buscar resumos de todos os jogadores da partida
        val listener = voteSummariesCollection
            .document(matchId)
            .collection("players")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val summaries = snapshot.documents.mapNotNull { doc ->
                        try {
                            val playerId = doc.id
                            val averageRating = doc.getDouble("averageRating") ?: 0.0
                            val totalVotes = doc.getLong("totalVotes")?.toInt() ?: 0

                            VoteSummary(
                                playerId = playerId,
                                matchId = matchId,
                                averageRating = averageRating,
                                totalVotes = totalVotes
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    trySend(summaries)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun hasUserVotedForMatch(userId: String, matchId: String): Flow<Boolean> = flow {
        try {
            // Buscar todos os votos do usuário para esta partida
            val querySnapshot = firestore.collectionGroup("user_votes")
                .whereEqualTo("userId", userId)
                .whereEqualTo("matchId", matchId)
                .limit(1) // Só precisamos saber se existe pelo menos um
                .get()
                .await()

            emit(!querySnapshot.isEmpty)
        } catch (e: Exception) {
            emit(false)
        }
    }

    /**
     * Atualiza o resumo de votos para um jogador em uma partida
     */
    private suspend fun updateVoteSummary(matchId: String, playerId: String) {
        try {
            // Obter todos os votos para este jogador nesta partida
            val querySnapshot = votesCollection
                .document(matchId)
                .collection("players")
                .document(playerId)
                .collection("user_votes")
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val votes = querySnapshot.documents.mapNotNull { doc ->
                    doc.getDouble("rating")?.toFloat()
                }

                // Calcular média
                val average = votes.average()
                val total = votes.size

                // Atualizar ou criar o documento de resumo
                val summaryRef = voteSummariesCollection
                    .document(matchId)
                    .collection("players")
                    .document(playerId)

                val summaryData = hashMapOf(
                    "averageRating" to average,
                    "totalVotes" to total,
                    "lastUpdated" to Date.from(Clock.System.now().toJavaInstant())
                )

                summaryRef.set(summaryData).await()
            }
        } catch (e: Exception) {
            println("Erro ao atualizar resumo de votos: ${e.message}")
        }
    }
}