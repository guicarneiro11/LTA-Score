package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction
import com.guicarneirodev.ltascore.domain.models.Vote
import com.guicarneirodev.ltascore.domain.models.VoteSummary
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
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

    // Escopo de IO para operações em background
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override suspend fun submitVote(vote: Vote) {
        try {
            // Referência para o documento do voto
            val voteRef = votesCollection
                .document(vote.matchId)
                .collection("players")
                .document(vote.playerId)
                .collection("user_votes")
                .document(vote.userId)

            // Dados do voto
            val voteData = hashMapOf(
                "matchId" to vote.matchId,
                "playerId" to vote.playerId,
                "userId" to vote.userId,
                "rating" to vote.rating,
                "timestamp" to Date.from(vote.timestamp.toJavaInstant())
            )

            // Salvar o voto
            voteRef.set(voteData).await()

            // Atualizar o resumo separadamente, após o voto ser salvo
            updateVoteSummary(vote.matchId, vote.playerId)
        } catch (e: Exception) {
            println("Erro ao enviar voto: ${e.message}")
            throw Exception("Erro ao enviar voto: ${e.message}")
        }
    }

    override suspend fun getUserVotes(userId: String): Flow<List<Vote>> = callbackFlow {
        // Encontrar os votos usando a coleção específica em vez de collectionGroup
        // Isso requer buscar match por match, o que não é ideal, mas evita erros de permissão
        val listener = votesCollection
            .limit(50) // Limitamos para evitar consultas muito grandes
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList<Vote>()) // Em caso de erro, enviamos uma lista vazia
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Aqui nós emitimos uma lista vazia temporariamente
                    // Em uma implementação completa, buscaríamos votos para cada match
                    trySend(emptyList<Vote>())
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
                    trySend(null)
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
        // Primeiro tentamos buscar os resumos pré-calculados
        val summariesListener = voteSummariesCollection
            .document(matchId)
            .collection("players")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    // Em caso de erro ou ausência de dados, emitimos uma lista vazia
                    // e lançamos uma coroutine para calcular em tempo real
                    trySend(emptyList())

                    // Usamos um escopo de coroutine para chamar a função suspensa
                    ioScope.launch {
                        try {
                            val calculatedSummaries = calculateRealTimeSummary(matchId)

                            // Tenta atualizar os resumos no Firestore para uso futuro do ranking
                            calculatedSummaries.forEach { summary ->
                                try {
                                    val summaryRef = voteSummariesCollection
                                        .document(matchId)
                                        .collection("players")
                                        .document(summary.playerId)

                                    val summaryData = hashMapOf(
                                        "averageRating" to summary.averageRating,
                                        "totalVotes" to summary.totalVotes,
                                        "lastUpdated" to Date.from(Clock.System.now().toJavaInstant())
                                    )

                                    summaryRef.set(summaryData).await()
                                } catch (e: Exception) {
                                    println("Erro ao salvar resumo calculado: ${e.message}")
                                }
                            }

                            trySend(calculatedSummaries)
                        } catch (e: Exception) {
                            // Em caso de erro no cálculo, já emitimos lista vazia acima
                            println("Erro ao calcular resumos em tempo real: ${e.message}")
                        }
                    }
                    return@addSnapshotListener
                }

                // Se temos resumos, usamos eles
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

        awaitClose {
            summariesListener.remove()
        }
    }

    // Função auxiliar para calcular resumos em tempo real
    private suspend fun calculateRealTimeSummary(matchId: String): List<VoteSummary> {
        val summaries = mutableListOf<VoteSummary>()

        try {
            // Obtemos IDs de jogadores para essa partida
            val playerIds = getPlayerIdsForMatch(matchId)

            // Para cada jogador, calculamos o resumo com base nos votos individuais
            for (playerId in playerIds) {
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

                    if (votes.isNotEmpty()) {
                        val average = votes.average()
                        summaries.add(
                            VoteSummary(
                                playerId = playerId,
                                matchId = matchId,
                                averageRating = average,
                                totalVotes = votes.size
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            println("Erro ao calcular resumos: ${e.message}")
        }

        return summaries
    }

    override suspend fun hasUserVotedForMatch(userId: String, matchId: String): Flow<Boolean> = flow {
        try {
            // Abordagem alternativa para verificar se o usuário votou
            // Verificamos um jogador específico por vez para evitar consultas collectionGroup

            // Obtemos a lista de jogadores usando os metadados da partida
            val playerIds = getPlayerIdsForMatch(matchId)

            var hasVoted = false

            // Verificamos se o usuário votou em qualquer um dos jogadores
            for (playerId in playerIds) {
                val voteRef = votesCollection
                    .document(matchId)
                    .collection("players")
                    .document(playerId)
                    .collection("user_votes")
                    .document(userId)

                val voteDoc = voteRef.get().await()
                if (voteDoc.exists()) {
                    hasVoted = true
                    break
                }
            }

            emit(hasVoted)
        } catch (e: Exception) {
            // Em caso de erro, assumimos que o usuário não votou
            println("Erro ao verificar votos do usuário: ${e.message}")
            emit(false)
        }
    }

    // Função auxiliar para obter IDs de jogadores de uma partida
    private suspend fun getPlayerIdsForMatch(matchId: String): List<String> {
        try {
            // Buscar os jogadores para os quais existem votos nesta partida
            val playersCollection = votesCollection.document(matchId).collection("players")
            val playersSnapshot = playersCollection.get().await()

            if (!playersSnapshot.isEmpty) {
                return playersSnapshot.documents.map { it.id }
            }

            // Se não encontrou, tenta no resumo
            val summaryPlayersCollection = voteSummariesCollection.document(matchId).collection("players")
            val summarySnapshot = summaryPlayersCollection.get().await()

            if (!summarySnapshot.isEmpty) {
                return summarySnapshot.documents.map { it.id }
            }
        } catch (e: Exception) {
            println("Erro ao buscar IDs de jogadores: ${e.message}")
        }

        // Se tudo falhar, retorna lista padrão
        return listOf(
            "player_ie_burdol", "player_ie_josedeodo", "player_ie_mireu",
            "player_ie_snaker", "player_ie_ackerman", "player_pain_wizer",
            "player_pain_cariok", "player_pain_roamer", "player_pain_titan",
            "player_pain_kuri"
        )
    }

    /**
     * Atualiza o resumo de votos para um jogador em uma partida dentro de uma transação
     */
    private fun updateVoteSummaryInTransaction(
        transaction: Transaction,
        matchId: String,
        playerId: String
    ) {
        try {
            // Referência para o documento de resumo
            val summaryRef = voteSummariesCollection
                .document(matchId)
                .collection("players")
                .document(playerId)

            // Observe que não podemos recuperar toda a coleção em uma transação
            // Em vez disso, vamos apenas criar um resumo básico para marcar que o voto foi processado
            // O resumo completo será atualizado posteriormente por um processo separado

            val summaryData = hashMapOf(
                "needsUpdate" to true,
                "lastUpdated" to Date.from(Clock.System.now().toJavaInstant())
            )

            // Atualizar resumo com informação de que precisa ser recalculado
            transaction.set(summaryRef, summaryData, SetOptions.merge())

            // Agendamos uma atualização do resumo fora da transação
            ioScope.launch {
                try {
                    delay(500) // Pequeno atraso para garantir que o voto foi salvo
                    updateVoteSummary(matchId, playerId)
                } catch (e: Exception) {
                    println("Erro ao atualizar resumo após transação: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Erro ao marcar resumo para atualização: ${e.message}")
        }
    }

    // Método para atualizar resumos fora de uma transação (usado pelo ranking)
    suspend fun updateVoteSummary(matchId: String, playerId: String) {
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

                if (votes.isNotEmpty()) {
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

                    try {
                        summaryRef.set(summaryData).await()
                    } catch (e: Exception) {
                        println("Erro ao salvar resumo de votos no Firestore: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            println("Erro ao calcular resumo de votos: ${e.message}")
        }
    }
}