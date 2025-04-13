package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.guicarneirodev.ltascore.data.datasource.local.MatchLocalDataSource
import com.guicarneirodev.ltascore.data.datasource.static.PlayersStaticDataSource
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
import java.util.Date

class FirebaseRankingRepository(
    firestore: FirebaseFirestore,
    private val playersDataSource: PlayersStaticDataSource,
    private val matchLocalDataSource: MatchLocalDataSource
) : RankingRepository {

    private val voteSummariesCollection = firestore.collection("vote_summaries")
    private val teamCache = mutableMapOf<String, TeamInfo>()

    // Classe interna para armazenar informações básicas do time
    private data class TeamInfo(
        val id: String,
        val name: String,
        val code: String,
        val image: String
    )

    init {
        // Inicializar o cache de times com base nos dados de partidas locais
        initTeamCache()
    }

    private fun initTeamCache() {
        // Liga Sul
        val allMatches = matchLocalDataSource.getMatches("lta_s")

        // Extrair informações de times de todas as partidas disponíveis
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

    override suspend fun getGeneralRanking(limit: Int): Flow<List<PlayerRankingItem>> = flow {
        try {
            // Buscar todos os resumos de votos e agregar por jogador
            val playerSummaries = getPlayerAverages()

            // Converter para o modelo de domínio
            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                createRankingItemFromSummary(playerId, data)
            }

            // Ordenar por avaliação média (decrescente)
            emit(rankingItems.sortedByDescending { it.averageRating })
        } catch (e: Exception) {
            println("Erro ao buscar ranking geral: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun getRankingByTeam(teamId: String): Flow<List<PlayerRankingItem>> = flow {
        try {
            // Buscar todos os resumos e depois filtrar por time
            val playerSummaries = getPlayerAverages()

            // Converter e filtrar apenas jogadores do time especificado
            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                val item = createRankingItemFromSummary(playerId, data)
                // Filtrar apenas os jogadores do time solicitado
                if (item?.teamId == teamId) item else null
            }

            emit(rankingItems.sortedByDescending { it.averageRating })
        } catch (e: Exception) {
            println("Erro ao buscar ranking por time: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun getRankingByPosition(position: PlayerPosition): Flow<List<PlayerRankingItem>> = flow {
        try {
            // Buscar todos os resumos e depois filtrar por posição
            val playerSummaries = getPlayerAverages()

            // Converter e filtrar apenas jogadores da posição especificada
            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                val item = createRankingItemFromSummary(playerId, data)
                // Filtrar apenas os jogadores da posição solicitada
                if (item?.position == position) item else null
            }

            emit(rankingItems.sortedByDescending { it.averageRating })
        } catch (e: Exception) {
            println("Erro ao buscar ranking por posição: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun getRankingByTimeFrame(timeFrame: TimeFrame): Flow<List<PlayerRankingItem>> = flow {
        try {
            val cutoffDate = getCutoffDateForTimeFrame(timeFrame)

            // Buscar resumos de votos mais recentes que a data de corte
            val playerSummaries = getPlayerAverages(cutoffDate)

            // Converter para o modelo de domínio
            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                createRankingItemFromSummary(playerId, data)
            }

            emit(rankingItems.sortedByDescending { it.averageRating })
        } catch (e: Exception) {
            println("Erro ao buscar ranking por período: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun getMostVotedRanking(limit: Int): Flow<List<PlayerRankingItem>> = flow {
        try {
            // Buscar todos os resumos de votos
            val playerSummaries = getPlayerAverages() // Buscamos mais para garantir

            // Converter para o modelo de domínio
            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                createRankingItemFromSummary(playerId, data)
            }

            // Ordenar por total de votos (decrescente) e limitar
            emit(rankingItems.sortedByDescending { it.totalVotes }.take(limit))
        } catch (e: Exception) {
            println("Erro ao buscar ranking de mais votados: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun refreshRankingData() {
        // Atualizar o cache de times
        initTeamCache()
    }

    // Função auxiliar para buscar médias agregadas por jogador
    private suspend fun getPlayerAverages(cutoffDate: Date? = null): Map<String, PlayerData> {
        val result = mutableMapOf<String, PlayerData>()

        // Buscar todos os documentos de resumos de votos
        val matchesQuery = voteSummariesCollection
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .limit(100) // Limitar para evitar consultas muito grandes

        val matchQuerySnapshot = matchesQuery.get().await()

        for (matchDoc in matchQuerySnapshot.documents) {
            val matchId = matchDoc.id

            // Para cada partida, buscar os resumos de jogadores
            val playersCollection = voteSummariesCollection
                .document(matchId)
                .collection("players")

            val playersQuerySnapshot = playersCollection.get().await()

            for (playerDoc in playersQuerySnapshot.documents) {
                val playerId = playerDoc.id
                val averageRating = playerDoc.getDouble("averageRating") ?: 0.0
                val totalVotes = playerDoc.getLong("totalVotes")?.toInt() ?: 0
                val lastUpdated = playerDoc.getDate("lastUpdated")

                // Verificar se está dentro do período de tempo solicitado
                if (cutoffDate != null && (lastUpdated == null || lastUpdated.before(cutoffDate))) {
                    continue
                }

                // Agregar os dados do jogador
                val playerData = result.getOrPut(playerId) {
                    PlayerData(
                        totalRating = 0.0,
                        totalVotesAcrossMatches = 0,
                        totalMatches = 0,
                        lastMatchDate = null
                    )
                }

                // Atualizar dados agregados
                playerData.totalRating += averageRating * totalVotes
                playerData.totalVotesAcrossMatches += totalVotes
                playerData.totalMatches++

                // Atualizar a data da última partida
                if (lastUpdated != null) {
                    val instant = Instant.fromEpochMilliseconds(lastUpdated.time)
                    if (playerData.lastMatchDate == null || instant > playerData.lastMatchDate!!) {
                        playerData.lastMatchDate = instant
                    }
                }
            }
        }

        // Calcular as médias finais
        result.forEach { (_, data) ->
            if (data.totalVotesAcrossMatches > 0) {
                data.averageRating = data.totalRating / data.totalVotesAcrossMatches
            }
        }

        return result
    }

    // Classe auxiliar para armazenar dados agregados de jogadores
    private data class PlayerData(
        var totalRating: Double,
        var totalVotesAcrossMatches: Int,
        var totalMatches: Int,
        var lastMatchDate: Instant?,
        var averageRating: Double = 0.0
    )

    // Função auxiliar para criar um item de ranking a partir dos dados agregados
    private fun createRankingItemFromSummary(playerId: String, data: PlayerData): PlayerRankingItem? {
        // Buscar dados do jogador da fonte estática
        val player = playersDataSource.getPlayerById(playerId) ?: return null

        // Buscar dados do time do cache
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

    // Função auxiliar para obter a data de corte com base no período
    private fun getCutoffDateForTimeFrame(timeFrame: TimeFrame): Date {
        val now = Clock.System.now()

        val cutoffInstant = when (timeFrame) {
            TimeFrame.CURRENT_WEEK -> now.minus(7, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            TimeFrame.CURRENT_MONTH -> now.minus(30, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            TimeFrame.ALL_TIME -> Instant.fromEpochMilliseconds(0) // Início dos tempos
        }

        // Converter de kotlinx.datetime.Instant para java.util.Date
        return Date(cutoffInstant.toEpochMilliseconds())
    }
}