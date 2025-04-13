package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
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
import kotlinx.datetime.toJavaInstant
import java.util.Date

class FirebaseRankingRepository(
    firestore: FirebaseFirestore,
    private val playersDataSource: PlayersStaticDataSource,
    private val matchLocalDataSource: MatchLocalDataSource
) : RankingRepository {

    private val voteSummariesCollection = firestore.collection("vote_summaries")
    private val votesCollection = firestore.collection("votes")
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
            println("Iniciando cálculo do ranking geral (limit=$limit)")

            // Primeiro tentamos via resumos pré-calculados
            val playerSummaries = getPlayerAverages(limit = limit)

            // Converter para o modelo de domínio
            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                createRankingItemFromSummary(playerId, data)
            }

            println("Ranking via resumos: ${rankingItems.size} jogadores")

            // Se temos menos de 15 itens no ranking e esperamos pelo menos 30,
            // provavelmente há um problema nos resumos, então recalculamos
            if (rankingItems.size < 15 && limit >= 30) {
                println("Poucos itens no ranking (${rankingItems.size}), recalculando...")

                // Buscar dados diretamente dos votos
                val recalculatedSummaries = calculateRealTimePlayerAverages(limit)
                val recalculatedItems = recalculatedSummaries.mapNotNull { (playerId, data) ->
                    createRankingItemFromSummary(playerId, data)
                }

                println("Ranking recalculado: ${recalculatedItems.size} jogadores")

                // Se temos mais dados recalculados, usamos eles
                if (recalculatedItems.size > rankingItems.size) {
                    emit(recalculatedItems.sortedByDescending { it.averageRating })
                    return@flow
                }
            }

            // Ordenar por avaliação média (decrescente)
            emit(rankingItems.sortedByDescending { it.averageRating })
        } catch (e: Exception) {
            println("Erro ao buscar ranking geral: ${e.message}")
            e.printStackTrace()
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
            val playerSummaries = getPlayerAverages(limit = limit) // Usando o parâmetro limit

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

        // Forçar atualização de toda a coleção vote_summaries
        try {
            // Buscar as partidas mais recentes (último mês)
            val cutoffDate = getCutoffDateForTimeFrame(TimeFrame.CURRENT_MONTH)

            // Buscar todas as partidas com votos
            val recentMatches = votesCollection
                .get()
                .await()
                .documents
                .map { it.id }

            // Atualizar cada partida
            var updatedCount = 0
            for (matchId in recentMatches) {
                try {
                    // Obter jogadores para esta partida
                    val playerIds = getPlayerIdsForMatch(matchId)

                    // Atualizar resumo para cada jogador
                    for (playerId in playerIds) {
                        updateVoteSummary(matchId, playerId)
                        updatedCount++
                    }
                } catch (e: Exception) {
                    println("Erro ao atualizar resumos para partida $matchId: ${e.message}")
                    // Continua para a próxima partida
                }
            }

            println("Atualização de ranking concluída: $updatedCount resumos atualizados")
        } catch (e: Exception) {
            println("Erro ao atualizar dados de ranking: ${e.message}")
        }
    }

    // Função auxiliar para buscar médias agregadas por jogador
    private suspend fun getPlayerAverages(cutoffDate: Date? = null, limit: Int = 100): Map<String, PlayerData> {
        val result = mutableMapOf<String, PlayerData>()

        try {
            // PASSO 1: Primeiro, vamos listar as partidas de uma forma diferente
            println("Verificando quais partidas têm resumos...")

            // Vamos buscar diretamente alguns IDs de partidas que sabemos que existem
            // a partir dos documentos que você mencionou
            val knownMatchIds = listOf(
                "114103277165171985", // ID que você mencionou que tem o player_pain_cariok
                "114103277165171991"  // ID que você mencionou que tem o player_loud_shini
            )

            // Também vamos tentar buscar partidas de outra maneira
            val partidas = mutableListOf<String>()

            // Tenta obter partidas da coleção votes
            try {
                val votesSnapshot = votesCollection.get().await()
                if (!votesSnapshot.isEmpty) {
                    partidas.addAll(votesSnapshot.documents.map { it.id })
                    println("Encontradas ${partidas.size} partidas na coleção votes")
                }
            } catch (e: Exception) {
                println("Erro ao buscar partidas da coleção votes: ${e.message}")
            }

            // Adiciona IDs conhecidos
            partidas.addAll(knownMatchIds)
            partidas.distinct() // Remove duplicatas

            println("Total de ${partidas.size} partidas para processar")

            // PASSO 2: Para cada partida, buscar resumos de jogadores
            for (matchId in partidas) {
                try {
                    println("Buscando jogadores para partida $matchId")

                    val playersRef = voteSummariesCollection
                        .document(matchId)
                        .collection("players")

                    val playersSnapshot = playersRef.get().await()

                    if (playersSnapshot.isEmpty) {
                        println("Nenhum jogador encontrado para partida $matchId")
                        continue
                    }

                    println("Encontrados ${playersSnapshot.size()} jogadores para partida $matchId")

                    // Para cada jogador, processamos o resumo
                    for (playerDoc in playersSnapshot.documents) {
                        val playerId = playerDoc.id
                        val averageRating = playerDoc.getDouble("averageRating") ?: 0.0
                        val totalVotes = playerDoc.getLong("totalVotes")?.toInt() ?: 0
                        val lastUpdated = playerDoc.getDate("lastUpdated")

                        // Verificar cutoff date
                        if (cutoffDate != null && (lastUpdated == null || lastUpdated.before(cutoffDate))) {
                            continue
                        }

                        // Debug
                        println("Jogador $playerId na partida $matchId: média $averageRating, $totalVotes votos")

                        // Agregar dados
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

                        // Atualizar data
                        if (lastUpdated != null) {
                            val instant = Instant.fromEpochMilliseconds(lastUpdated.time)
                            if (playerData.lastMatchDate == null || instant > playerData.lastMatchDate!!) {
                                playerData.lastMatchDate = instant
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Erro ao processar partida $matchId: ${e.message}")
                    e.printStackTrace()
                }
            }

            // Calcular médias finais
            result.forEach { (playerId, data) ->
                if (data.totalVotesAcrossMatches > 0) {
                    data.averageRating = data.totalRating / data.totalVotesAcrossMatches
                    println("Média final para jogador $playerId: ${data.averageRating} (${data.totalVotesAcrossMatches} votos)")
                }
            }

            println("Gerado ranking para ${result.size} jogadores")

            return result
        } catch (e: Exception) {
            println("Erro geral ao calcular ranking: ${e.message}")
            e.printStackTrace()
            return emptyMap()
        }
    }

    // Método novo: Calcula médias diretamente dos votos individuais (fallback)
    private suspend fun calculateRealTimePlayerAverages(limit: Int): Map<String, PlayerData> {
        val result = mutableMapOf<String, PlayerData>()

        try {
            // Abordagem 1: Usar todos os jogadores disponíveis da fonte estática
            val allPlayers = playersDataSource.getAllPlayers()

            println("Calculando ranking para ${allPlayers.size} jogadores")

            // Para cada jogador
            for (player in allPlayers) {
                val playerId = player.id
                val playerVotes = mutableListOf<Float>()
                var totalMatches = 0
                var lastMatchDate: Instant? = null

                try {
                    // Buscar partidas que têm este jogador
                    val matchesWithPlayer = votesCollection
                        .get()
                        .await()
                        .documents
                        .filter { matchDoc ->
                            try {
                                // Verificar se existe a subcolecção players/playerId
                                val hasPlayerVotes = matchDoc.reference
                                    .collection("players")
                                    .document(playerId)
                                    .collection("user_votes")
                                    .limit(1)
                                    .get()
                                    .await()
                                    .size() > 0

                                hasPlayerVotes
                            } catch (e: Exception) {
                                false
                            }
                        }
                        .map { it.id }

                    println("Jogador $playerId: encontradas ${matchesWithPlayer.size} partidas com votos")

                    // Para cada partida
                    for (matchId in matchesWithPlayer) {
                        try {
                            // Buscar votos deste jogador nesta partida
                            val votesSnapshot = votesCollection
                                .document(matchId)
                                .collection("players")
                                .document(playerId)
                                .collection("user_votes")
                                .get()
                                .await()

                            if (!votesSnapshot.isEmpty) {
                                // Extrair valores de rating
                                val matchVotes = votesSnapshot.documents
                                    .mapNotNull { doc ->
                                        doc.getDouble("rating")?.toFloat()
                                    }

                                if (matchVotes.isNotEmpty()) {
                                    // Adicionar todos os votos à lista
                                    playerVotes.addAll(matchVotes)
                                    totalMatches++

                                    // Verificar data da partida
                                    val lastVoteDate = votesSnapshot.documents
                                        .mapNotNull { it.getDate("timestamp") }
                                        .maxByOrNull { it.time }

                                    if (lastVoteDate != null) {
                                        val instant = Instant.fromEpochMilliseconds(lastVoteDate.time)
                                        if (lastMatchDate == null || instant > lastMatchDate) {
                                            lastMatchDate = instant
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("Erro ao buscar votos de $playerId na partida $matchId: ${e.message}")
                            // Continuar para a próxima partida
                        }
                    }

                    // Calcular média se temos votos
                    if (playerVotes.isNotEmpty()) {
                        val average = playerVotes.average()
                        val total = playerVotes.size

                        result[playerId] = PlayerData(
                            totalRating = average * total,
                            totalVotesAcrossMatches = total,
                            totalMatches = totalMatches,
                            lastMatchDate = lastMatchDate,
                            averageRating = average
                        )

                        println("Jogador $playerId: ${playerVotes.size} votos, média $average")
                    }
                } catch (e: Exception) {
                    println("Erro ao processar jogador $playerId: ${e.message}")
                    // Continuar para o próximo jogador
                }
            }

            println("Calculados rankings para ${result.size} jogadores")

            return result
        } catch (e: Exception) {
            println("Erro ao calcular médias em tempo real: ${e.message}")
            e.printStackTrace()
            return emptyMap()
        }
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

    // Função auxiliar para obter IDs de jogadores de uma partida
    private suspend fun getPlayerIdsForMatch(matchId: String): List<String> {
        try {
            // Buscar as coleções de jogadores para esta partida
            val playersSnapshot = votesCollection
                .document(matchId)
                .collection("players")
                .get()
                .await()

            return playersSnapshot.documents.map { it.id }
        } catch (e: Exception) {
            // Se não conseguir buscar da collections, tenta uma lista padrão
            val standardPlayers = listOf(
                "player_ie_burdol", "player_ie_josedeodo", "player_ie_mireu",
                "player_ie_snaker", "player_ie_ackerman", "player_pain_wizer",
                "player_pain_cariok", "player_pain_roamer", "player_pain_titan",
                "player_pain_kuri"
            )

            return standardPlayers
        }
    }

    // Função para atualizar resumo de votos
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