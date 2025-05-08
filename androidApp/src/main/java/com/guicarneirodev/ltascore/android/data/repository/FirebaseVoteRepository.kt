package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.guicarneirodev.ltascore.data.datasource.static.PlayersDataSource
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.Vote
import com.guicarneirodev.ltascore.domain.models.VoteSummary
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
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
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val playersDataSource: PlayersDataSource
) : VoteRepository {

    private val votesCollection = firestore.collection("votes")
    private val voteSummariesCollection = firestore.collection("vote_summaries")

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override suspend fun getUserVotes(userId: String): Flow<List<Vote>> = callbackFlow {
        val listener = votesCollection
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList<Vote>())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    trySend(emptyList<Vote>())
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun getUserVoteForPlayer(userId: String, matchId: String, playerId: String): Flow<Vote?> = callbackFlow {
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
                    } catch (_: Exception) {
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
        val summariesListener = voteSummariesCollection
            .document(matchId)
            .collection("players")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    trySend(emptyList())

                    ioScope.launch {
                        try {
                            val calculatedSummaries = calculateRealTimeSummary(matchId)

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
                            println("Erro ao calcular resumos em tempo real: ${e.message}")
                        }
                    }
                    return@addSnapshotListener
                }

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
                    } catch (_: Exception) {
                        null
                    }
                }

                trySend(summaries)
            }

        awaitClose {
            summariesListener.remove()
        }
    }

    private suspend fun calculateRealTimeSummary(matchId: String): List<VoteSummary> {
        val summaries = mutableListOf<VoteSummary>()

        try {
            val playerIds = getPlayerIdsForMatch(matchId)

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
            val playerIds = getPlayerIdsForMatch(matchId)

            var hasVoted = false

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
            println("Erro ao verificar votos do usuário: ${e.message}")
            emit(false)
        }
    }

    private suspend fun getPlayerIdsForMatch(matchId: String): List<String> {
        try {
            val playersCollection = votesCollection.document(matchId).collection("players")
            val playersSnapshot = playersCollection.get().await()

            if (!playersSnapshot.isEmpty) {
                return playersSnapshot.documents.map { it.id }
            }

            val summaryPlayersCollection = voteSummariesCollection.document(matchId).collection("players")
            val summarySnapshot = summaryPlayersCollection.get().await()

            if (!summarySnapshot.isEmpty) {
                return summarySnapshot.documents.map { it.id }
            }
        } catch (e: Exception) {
            println("Erro ao buscar IDs de jogadores: ${e.message}")
        }

        return listOf(
            "player_ie_burdol", "player_ie_josedeodo", "player_ie_mireu",
            "player_ie_snaker", "player_ie_ackerman", "player_pain_wizer",
            "player_pain_cariok", "player_pain_roamer", "player_pain_titan",
            "player_pain_kuri"
        )
    }

    suspend fun updateVoteSummary(matchId: String, playerId: String) {
        try {
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
                    val total = votes.size

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

    override suspend fun submitVote(vote: Vote) {
        try {
            val voteRef = votesCollection
                .document(vote.matchId)
                .collection("players")
                .document(vote.playerId)
                .collection("user_votes")
                .document(vote.userId)

            val voteData = hashMapOf(
                "matchId" to vote.matchId,
                "playerId" to vote.playerId,
                "userId" to vote.userId,
                "rating" to vote.rating,
                "timestamp" to Date.from(vote.timestamp.toJavaInstant())
            )

            voteRef.set(voteData).await()

            updateVoteSummary(vote.matchId, vote.playerId)

            val matchInfo = getMatchMetadata(vote.matchId)
            val playerInfo = getPlayerMetadata(vote.playerId)

            if (matchInfo != null && playerInfo != null) {
                val historyItem = UserVoteHistoryItem(
                    id = "${vote.matchId}_${vote.playerId}",
                    matchId = vote.matchId,
                    matchDate = matchInfo.date,
                    playerId = vote.playerId,
                    playerName = playerInfo.name,
                    playerNickname = playerInfo.nickname,
                    playerImage = playerInfo.imageUrl,
                    playerPosition = playerInfo.position,
                    teamId = playerInfo.teamId,
                    teamName = matchInfo.teams[playerInfo.teamId]?.name ?: "",
                    teamCode = matchInfo.teams[playerInfo.teamId]?.code ?: "",
                    teamImage = matchInfo.teams[playerInfo.teamId]?.image ?: "",
                    opponentTeamCode = matchInfo.opponentCode,
                    rating = vote.rating,
                    timestamp = vote.timestamp
                )

                addVoteToUserHistory(vote.userId, historyItem)
            }
        } catch (e: Exception) {
            println("Erro ao enviar voto: ${e.message}")
            throw Exception("Erro ao enviar voto: ${e.message}")
        }
    }

    override suspend fun addVoteToUserHistory(userId: String, historyItem: UserVoteHistoryItem) {
        try {
            println("Adicionando voto ao histórico do usuário $userId: ${historyItem.playerNickname}")

            val historyRef = firestore
                .collection("user_vote_history")
                .document(userId)
                .collection("votes")
                .document(historyItem.id)

            val historyData = hashMapOf(
                "matchId" to historyItem.matchId,
                "matchDate" to Date.from(historyItem.matchDate.toJavaInstant()),
                "playerId" to historyItem.playerId,
                "playerName" to historyItem.playerName,
                "playerNickname" to historyItem.playerNickname,
                "playerImage" to historyItem.playerImage,
                "playerPosition" to historyItem.playerPosition.name,
                "teamId" to historyItem.teamId,
                "teamName" to historyItem.teamName,
                "teamCode" to historyItem.teamCode,
                "teamImage" to historyItem.teamImage,
                "opponentTeamCode" to historyItem.opponentTeamCode,
                "rating" to historyItem.rating,
                "timestamp" to Date.from(historyItem.timestamp.toJavaInstant())
            )

            historyRef.set(historyData).await()
            println("Voto adicionado com sucesso ao histórico")
        } catch (e: Exception) {
            println("ERRO ao salvar histórico de voto: ${e.message}")
            e.printStackTrace()
        }
    }

    override suspend fun getUserVoteHistory(userId: String): Flow<List<UserVoteHistoryItem>> = callbackFlow {
        try {
            println("Buscando histórico de votos para usuário $userId")

            val listener = firestore
                .collection("user_vote_history")
                .document(userId)
                .collection("votes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("Erro ao observar histórico: ${error.message}")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        println("Nenhum voto encontrado no histórico")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val historyItems = snapshot.documents.mapNotNull { doc ->
                        try {
                            val matchId = doc.getString("matchId") ?: return@mapNotNull null
                            val matchDateTimestamp = doc.getDate("matchDate")
                            val playerId = doc.getString("playerId") ?: return@mapNotNull null
                            val playerName = doc.getString("playerName") ?: ""
                            val playerNickname = doc.getString("playerNickname") ?: ""
                            val playerImage = doc.getString("playerImage") ?: ""
                            val playerPositionStr = doc.getString("playerPosition") ?: "TOP"
                            val teamId = doc.getString("teamId") ?: ""
                            val teamName = doc.getString("teamName") ?: ""
                            val teamCode = doc.getString("teamCode") ?: ""
                            val teamImage = doc.getString("teamImage") ?: ""
                            val opponentTeamCode = doc.getString("opponentTeamCode") ?: ""
                            val rating = doc.getDouble("rating")?.toFloat() ?: 0f
                            val timestampDate = doc.getDate("timestamp")

                            val playerPosition = try {
                                PlayerPosition.valueOf(playerPositionStr)
                            } catch (_: Exception) {
                                PlayerPosition.TOP
                            }

                            val matchDate = if (matchDateTimestamp != null) {
                                Instant.fromEpochMilliseconds(matchDateTimestamp.time)
                            } else {
                                Clock.System.now()
                            }

                            val timestamp = if (timestampDate != null) {
                                Instant.fromEpochMilliseconds(timestampDate.time)
                            } else {
                                Clock.System.now()
                            }

                            val item = UserVoteHistoryItem(
                                id = doc.id,
                                matchId = matchId,
                                matchDate = matchDate,
                                playerId = playerId,
                                playerName = playerName,
                                playerNickname = playerNickname,
                                playerImage = playerImage,
                                playerPosition = playerPosition,
                                teamId = teamId,
                                teamName = teamName,
                                teamCode = teamCode,
                                teamImage = teamImage,
                                opponentTeamCode = opponentTeamCode,
                                rating = rating,
                                timestamp = timestamp
                            )

                            println("Voto encontrado para jogador: ${item.playerNickname}, rating: ${item.rating}")
                            item
                        } catch (e: Exception) {
                            println("Erro ao processar documento do histórico: ${e.message}")
                            null
                        }
                    }

                    println("Total de ${historyItems.size} votos encontrados no histórico")
                    trySend(historyItems)
                }

            awaitClose {
                listener.remove()
            }
        } catch (e: Exception) {
            println("Erro crítico ao buscar histórico de votos: ${e.message}")
            e.printStackTrace()
            trySend(emptyList())
            close(e)
        }
    }

    private suspend fun getMatchMetadata(matchId: String): MatchMetadata? {
        try {
            val matchDoc = firestore
                .collection("match_metadata")
                .document(matchId)
                .get()
                .await()

            if (matchDoc.exists()) {
                val date = matchDoc.getDate("date")?.toInstant() ?: Clock.System.now().toJavaInstant()
                val teamsMap = mutableMapOf<String, TeamInfo>()

                val teamsData = matchDoc.get("teams") as? Map<*, *>
                teamsData?.forEach { (teamId, data) ->
                    if (teamId is String && data is Map<*, *>) {
                        teamsMap[teamId] = TeamInfo(
                            id = teamId,
                            name = data["name"] as? String ?: "",
                            code = data["code"] as? String ?: "",
                            image = data["image"] as? String ?: ""
                        )
                    }
                }

                return MatchMetadata(
                    id = matchId,
                    date = Instant.fromEpochMilliseconds(date.toEpochMilli()),
                    teams = teamsMap,
                    opponentCode = matchDoc.getString("opponentCode") ?: ""
                )
            }

            return null
        } catch (e: Exception) {
            println("Erro ao buscar metadados da partida: ${e.message}")
            return null
        }
    }

    private fun getPlayerMetadata(playerId: String): PlayerMetadata? {
        try {
            val player = playersDataSource.getPlayerById(playerId)

            return player?.let {
                PlayerMetadata(
                    id = it.id,
                    name = it.name,
                    nickname = it.nickname,
                    imageUrl = it.imageUrl,
                    position = it.position,
                    teamId = it.teamId
                )
            }
        } catch (e: Exception) {
            println("Erro ao buscar metadados do jogador: ${e.message}")
            return null
        }
    }

    private data class MatchMetadata(
        val id: String,
        val date: Instant,
        val teams: Map<String, TeamInfo>,
        val opponentCode: String
    )

    private data class TeamInfo(
        val id: String,
        val name: String,
        val code: String,
        val image: String
    )

    private data class PlayerMetadata(
        val id: String,
        val name: String,
        val nickname: String,
        val imageUrl: String,
        val position: PlayerPosition,
        val teamId: String
    )
}