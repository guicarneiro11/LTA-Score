package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.ltascore.data.datasource.local.MatchLocalDataSource
import com.guicarneirodev.ltascore.data.datasource.static.PlayersStaticDataSource
import com.guicarneirodev.ltascore.domain.models.MatchState
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.PlayerRankingItem
import com.guicarneirodev.ltascore.domain.models.TimeFrame
import com.guicarneirodev.ltascore.domain.repository.RankingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaInstant
import java.util.Date

class FirebaseRankingRepository(
    firestore: FirebaseFirestore,
    private val playersDataSource: PlayersStaticDataSource,
    private val matchLocalDataSource: MatchLocalDataSource
) : RankingRepository {

    private val voteSummariesCollection = firestore.collection("vote_summaries")
    private val teamCache = mutableMapOf<String, TeamInfo>()
    private val votesCollection = firestore.collection("votes")

    private data class TeamInfo(
        val id: String,
        val name: String,
        val code: String,
        val image: String
    )

    init {
        initTeamCache()
    }

    private fun initTeamCache() {
        val allMatches = matchLocalDataSource.getMatches("lta_s")

        allMatches.forEach { match ->
            match.teams.forEach { team ->
                if (!teamCache.containsKey(team.id)) {
                    teamCache[team.id] = TeamInfo(
                        id = team.id,
                        name = team.name,
                        code = team.code,
                        image = team.imageUrl
                    )
                }
            }
        }
    }

    private suspend fun getAllMatchPlayerSummaries(): Map<String, PlayerData> {
        val result = mutableMapOf<String, PlayerData>()

        try {
            println("Calculando ranking com agregação de todas as partidas...")

            val matchIds = mutableSetOf<String>()

            val ltaSulMatches = matchLocalDataSource.getMatches("lta_s")
            val ltaNorteMatches = matchLocalDataSource.getMatches("lta_n")

            val allMatches = ltaSulMatches + ltaNorteMatches
            val completedMatches = allMatches.filter { it.state == MatchState.COMPLETED }

            completedMatches.forEach { match ->
                matchIds.add(match.id)
            }

            println("📊 Encontradas ${matchIds.size} partidas do cache local")

            if (matchIds.isEmpty()) {
                try {
                    println("Buscando partidas da coleção votes...")
                    val votesSnapshot = votesCollection.get().await()

                    if (votesSnapshot != null && !votesSnapshot.isEmpty) {
                        for (doc in votesSnapshot.documents) {
                            matchIds.add(doc.id)
                        }
                        println("✅ Encontradas ${matchIds.size} partidas na coleção votes")
                    } else {
                        println("⚠️ Nenhuma partida encontrada na coleção votes")
                    }
                } catch (e: Exception) {
                    println("❌ Erro ao acessar coleção votes: ${e.message}")
                }
            }

            if (matchIds.isEmpty()) {
                println("⚠️ Nenhuma partida encontrada das fontes primárias. Gerando IDs potenciais...")

                val baseIds = listOf(
                    "114103277164844275",
                    "114103277165106421",
                    "114103277165171985"
                )

                baseIds.forEach { baseId ->
                    matchIds.add(baseId)
                    for (i in 1..5) {
                        val lastDigits = baseId.takeLast(4).toInt() + (i * 2)
                        val newId = baseId.substring(0, baseId.length - 4) + lastDigits.toString().padStart(4, '0')
                        matchIds.add(newId)
                    }
                }

                println("📊 Gerados ${matchIds.size} IDs potenciais de partidas")
            }

            println("📊 Total de ${matchIds.size} partidas para processar")

            var matchesProcessed = 0
            var playersFound = 0

            for (matchId in matchIds) {
                try {
                    val playersRef = voteSummariesCollection
                        .document(matchId)
                        .collection("players")

                    val playersSnapshot = playersRef.get().await()

                    if (playersSnapshot != null && !playersSnapshot.isEmpty) {
                        matchesProcessed++
                        val playerCount = playersSnapshot.size()
                        playersFound += playerCount
                        println("✅ Partida $matchId: encontrados $playerCount jogadores")

                        for (playerDoc in playersSnapshot.documents) {
                            val playerId = playerDoc.id
                            val averageRating = playerDoc.getDouble("averageRating") ?: 0.0
                            val totalVotes = playerDoc.getLong("totalVotes")?.toInt() ?: 0
                            val lastUpdated = playerDoc.getDate("lastUpdated")

                            if (totalVotes > 0) {
                                println("🎮 Jogador $playerId: rating $averageRating, $totalVotes votos")

                                val playerData = result.getOrPut(playerId) {
                                    PlayerData(
                                        totalRating = 0.0,
                                        totalVotesAcrossMatches = 0,
                                        totalMatches = 0,
                                        lastMatchDate = null
                                    )
                                }

                                playerData.totalRating += averageRating * totalVotes
                                playerData.totalVotesAcrossMatches += totalVotes
                                playerData.totalMatches++

                                if (lastUpdated != null) {
                                    val instant = Instant.fromEpochMilliseconds(lastUpdated.time)
                                    if (playerData.lastMatchDate == null || instant > playerData.lastMatchDate!!) {
                                        playerData.lastMatchDate = instant
                                    }
                                }
                            }
                        }
                    } else {
                        println("⚠️ Partida $matchId: não tem jogadores ou não existe")
                    }
                } catch (e: Exception) {
                    println("❌ Erro ao processar partida $matchId: ${e.message}")
                }
            }

            println("📊 Resumo: Processadas $matchesProcessed partidas, encontrados $playersFound jogadores totais")

            result.forEach { (playerId, data) ->
                if (data.totalVotesAcrossMatches > 0) {
                    data.averageRating = data.totalRating / data.totalVotesAcrossMatches
                    println("🏆 FINAL: Jogador $playerId - Média ${data.averageRating} com ${data.totalVotesAcrossMatches} votos em ${data.totalMatches} partidas")
                }
            }

            println("✅ Ranking completo: ${result.size} jogadores agregados com sucesso")

            return result
        } catch (e: Exception) {
            println("🛑 ERRO CRÍTICO: ${e.message}")
            e.printStackTrace()
            return emptyMap()
        }
    }

    override suspend fun getGeneralRanking(limit: Int): Flow<List<PlayerRankingItem>> = flow {
        try {
            println("Calculando ranking com agregação de todas as partidas...")

            val playerSummaries = getAllMatchPlayerSummaries()

            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                createRankingItemFromSummary(playerId, data)
            }

            println("Ranking gerado: ${rankingItems.size} jogadores com todos os votos agregados")

            val sortedItems = rankingItems.sortedByDescending { it.averageRating }

            emit(sortedItems.take(limit))
        } catch (e: Exception) {
            println("Erro ao calcular ranking: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override suspend fun getRankingByTeam(teamId: String): Flow<List<PlayerRankingItem>> = flow {
        try {
            println("Calculando ranking por time: $teamId")

            val playerSummaries = getAllMatchPlayerSummaries()

            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                val item = createRankingItemFromSummary(playerId, data)
                if (item?.teamId == teamId) item else null
            }

            println("Ranking por time $teamId: ${rankingItems.size} jogadores")

            emit(rankingItems.sortedByDescending { it.averageRating })
        } catch (e: Exception) {
            println("Erro ao buscar ranking por time: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override suspend fun getRankingByPosition(position: PlayerPosition): Flow<List<PlayerRankingItem>> = flow {
        try {
            println("Calculando ranking por posição: $position")

            val playerSummaries = getAllMatchPlayerSummaries()

            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                val item = createRankingItemFromSummary(playerId, data)
                if (item?.position == position) item else null
            }

            println("Ranking por posição $position: ${rankingItems.size} jogadores")

            emit(rankingItems.sortedByDescending { it.averageRating })
        } catch (e: Exception) {
            println("Erro ao buscar ranking por posição: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override suspend fun getRankingByTimeFrame(timeFrame: TimeFrame): Flow<List<PlayerRankingItem>> = flow {
        try {
            println("Calculando ranking por período: $timeFrame")
            val cutoffDate = getCutoffDateForTimeFrame(timeFrame)

            val allPlayerSummaries = getAllMatchPlayerSummaries()

            val filteredSummaries = allPlayerSummaries.filter { (_, data) ->
                data.lastMatchDate?.let { lastDate ->
                    val lastMatchDate = Date(lastDate.toEpochMilliseconds())
                    lastMatchDate.after(cutoffDate) || lastMatchDate == cutoffDate
                } == true
            }

            val rankingItems = filteredSummaries.mapNotNull { (playerId, data) ->
                createRankingItemFromSummary(playerId, data)
            }

            println("Ranking por período $timeFrame: ${rankingItems.size} jogadores")

            emit(rankingItems.sortedByDescending { it.averageRating })
        } catch (e: Exception) {
            println("Erro ao buscar ranking por período: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override suspend fun getMostVotedRanking(limit: Int): Flow<List<PlayerRankingItem>> = flow {
        try {
            println("Calculando ranking por mais votados (limit=$limit)")

            val playerSummaries = getAllMatchPlayerSummaries()

            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                createRankingItemFromSummary(playerId, data)
            }

            println("Ranking por votos: ${rankingItems.size} jogadores")

            emit(rankingItems.sortedByDescending { it.totalVotes }.take(limit))
        } catch (e: Exception) {
            println("Erro ao buscar ranking de mais votados: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override suspend fun refreshRankingData() {
        initTeamCache()

        try {
            getCutoffDateForTimeFrame(TimeFrame.CURRENT_MONTH)

            val recentMatches = votesCollection
                .get()
                .await()
                .documents
                .map { it.id }

            var updatedCount = 0
            for (matchId in recentMatches) {
                try {
                    val playerIds = getPlayerIdsForMatch(matchId)

                    for (playerId in playerIds) {
                        updateVoteSummary(matchId, playerId)
                        updatedCount++
                    }
                } catch (e: Exception) {
                    println("Erro ao atualizar resumos para partida $matchId: ${e.message}")
                }
            }

            println("Atualização de ranking concluída: $updatedCount resumos atualizados")
        } catch (e: Exception) {
            println("Erro ao atualizar dados de ranking: ${e.message}")
        }
    }

    private data class PlayerData(
        var totalRating: Double,
        var totalVotesAcrossMatches: Int,
        var totalMatches: Int,
        var lastMatchDate: Instant?,
        var averageRating: Double = 0.0
    )

    private fun createRankingItemFromSummary(playerId: String, data: PlayerData): PlayerRankingItem? {
        val player = playersDataSource.getPlayerById(playerId) ?: return null

        val teamId = player.teamId
        val teamInfo = teamCache[teamId] ?: return null

        return PlayerRankingItem(
            player = player,
            averageRating = data.averageRating,
            totalVotes = data.totalVotesAcrossMatches,
            teamId = teamId,
            teamName = teamInfo.name,
            teamCode = teamInfo.code,
            teamImage = teamInfo.image,
            position = player.position,
            lastMatchDate = data.lastMatchDate
        )
    }

    private fun getCutoffDateForTimeFrame(timeFrame: TimeFrame): Date {
        val now = Clock.System.now()

        val cutoffInstant = when (timeFrame) {
            TimeFrame.CURRENT_WEEK -> now.minus(
                7, DateTimeUnit.DAY, TimeZone.currentSystemDefault()
            )

            TimeFrame.CURRENT_MONTH -> now.minus(
                30, DateTimeUnit.DAY, TimeZone.currentSystemDefault()
            )

            TimeFrame.ALL_TIME -> Instant.fromEpochMilliseconds(0)
        }

        return Date(cutoffInstant.toEpochMilliseconds())
    }

    private suspend fun getPlayerIdsForMatch(matchId: String): List<String> {
        try {
            val playersSnapshot = votesCollection
                .document(matchId)
                .collection("players")
                .get()
                .await()

            return playersSnapshot.documents.map { it.id }
        } catch (_: Exception) {
            val standardPlayers = listOf(
                "player_ie_burdol", "player_ie_josedeodo", "player_ie_mireu",
                "player_ie_snaker", "player_ie_ackerman", "player_pain_wizer",
                "player_pain_cariok", "player_pain_roamer", "player_pain_titan",
                "player_pain_kuri"
            )

            return standardPlayers
        }
    }

    private suspend fun updateVoteSummary(matchId: String, playerId: String) {
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
        } catch (e: Exception) {
            println("Erro ao calcular resumo de votos: ${e.message}")
        }
    }
}