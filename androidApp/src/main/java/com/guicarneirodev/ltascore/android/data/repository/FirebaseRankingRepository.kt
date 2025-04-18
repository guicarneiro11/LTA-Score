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

    private suspend fun getAllMatchPlayerSummaries(): Map<String, PlayerData> {
        val result = mutableMapOf<String, PlayerData>()

        try {
            println("Calculando ranking com agregação de todas as partidas...")

            // Obtém IDs de partidas do cache local em vez de usar lista hardcoded
            val matchIds = mutableSetOf<String>()

            // Primeiro tenta obter as partidas da LTA Sul
            val ltaSulMatches = matchLocalDataSource.getMatches("lta_s")
            val ltaNorteMatches = matchLocalDataSource.getMatches("lta_n")

            // Adiciona os IDs de todas as partidas completadas
            val allMatches = ltaSulMatches + ltaNorteMatches
            val completedMatches = allMatches.filter { it.state == MatchState.COMPLETED }

            completedMatches.forEach { match ->
                matchIds.add(match.id)
            }

            println("📊 Encontradas ${matchIds.size} partidas do cache local")

            // Se não encontrou nenhuma partida, tenta usar a coleção votes
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

            // Se ainda não encontrou, cria uma lista com partidas potenciais
            if (matchIds.isEmpty()) {
                println("⚠️ Nenhuma partida encontrada das fontes primárias. Gerando IDs potenciais...")

                // Cria IDs baseados em uma sequência lógica observada nos IDs existentes
                // Os IDs parecem seguir um padrão como "114103277164844275" e incrementam
                val baseIds = listOf(
                    "114103277164844275", // Base para semana 1
                    "114103277165106421", // Base para semana 2
                    "114103277165171985"  // Base para semana 3
                )

                // Gera variações baseadas nos IDs base
                baseIds.forEach { baseId ->
                    matchIds.add(baseId)
                    // Adiciona algumas variações incrementando o final do ID
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

            // Para cada ID de partida encontrado
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

                        // Processar cada jogador
                        for (playerDoc in playersSnapshot.documents) {
                            val playerId = playerDoc.id
                            val averageRating = playerDoc.getDouble("averageRating") ?: 0.0
                            val totalVotes = playerDoc.getLong("totalVotes")?.toInt() ?: 0
                            val lastUpdated = playerDoc.getDate("lastUpdated")

                            if (totalVotes > 0) {
                                println("🎮 Jogador $playerId: rating $averageRating, $totalVotes votos")

                                // Agregar os dados do jogador
                                val playerData = result.getOrPut(playerId) {
                                    PlayerData(
                                        totalRating = 0.0,
                                        totalVotesAcrossMatches = 0,
                                        totalMatches = 0,
                                        lastMatchDate = null
                                    )
                                }

                                // Atualizar dados para média ponderada
                                playerData.totalRating += averageRating * totalVotes
                                playerData.totalVotesAcrossMatches += totalVotes
                                playerData.totalMatches++

                                // Atualizar data da última partida
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

            // Calcular médias finais
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

            // Usar o novo método para buscar e agregar dados de todas as partidas
            val playerSummaries = getAllMatchPlayerSummaries()

            // Converter para o modelo de domínio
            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                createRankingItemFromSummary(playerId, data)
            }

            println("Ranking gerado: ${rankingItems.size} jogadores com todos os votos agregados")

            // Ordenar por avaliação média ponderada (decrescente)
            val sortedItems = rankingItems.sortedByDescending { it.averageRating }

            // Aplicar limite após ordenação
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

            // Usar o mesmo método que está funcionando para getAllMatchPlayerSummaries
            val playerSummaries = getAllMatchPlayerSummaries()

            // Converter para o modelo de domínio e filtrar por time
            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                val item = createRankingItemFromSummary(playerId, data)
                // Filtrar apenas os jogadores do time solicitado
                if (item?.teamId == teamId) item else null
            }

            println("Ranking por time $teamId: ${rankingItems.size} jogadores")

            // Ordenar por avaliação média (decrescente)
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

            // Usar o mesmo método que está funcionando
            val playerSummaries = getAllMatchPlayerSummaries()

            // Converter para o modelo de domínio e filtrar por posição
            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                val item = createRankingItemFromSummary(playerId, data)
                // Filtrar apenas os jogadores da posição solicitada
                if (item?.position == position) item else null
            }

            println("Ranking por posição $position: ${rankingItems.size} jogadores")

            // Ordenar por avaliação média (decrescente)
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

            // Usar o mesmo método que está funcionando, mas filtrar por data depois
            val allPlayerSummaries = getAllMatchPlayerSummaries()

            // Filtrar jogadores com partidas dentro do período solicitado
            val filteredSummaries = allPlayerSummaries.filter { (_, data) ->
                // Se o jogador tem data da última partida e está dentro do período
                data.lastMatchDate?.let { lastDate ->
                    val lastMatchDate = Date(lastDate.toEpochMilliseconds())
                    lastMatchDate.after(cutoffDate) || lastMatchDate == cutoffDate
                } == true
            }

            // Converter para o modelo de domínio
            val rankingItems = filteredSummaries.mapNotNull { (playerId, data) ->
                createRankingItemFromSummary(playerId, data)
            }

            println("Ranking por período $timeFrame: ${rankingItems.size} jogadores")

            // Ordenar por avaliação média (decrescente)
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

            // Usar o mesmo método que está funcionando
            val playerSummaries = getAllMatchPlayerSummaries()

            // Converter para o modelo de domínio
            val rankingItems = playerSummaries.mapNotNull { (playerId, data) ->
                createRankingItemFromSummary(playerId, data)
            }

            println("Ranking por votos: ${rankingItems.size} jogadores")

            // Ordenar por total de votos (decrescente) e limitar
            emit(rankingItems.sortedByDescending { it.totalVotes }.take(limit))
        } catch (e: Exception) {
            println("Erro ao buscar ranking de mais votados: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override suspend fun refreshRankingData() {
        // Atualizar o cache de times
        initTeamCache()

        // Forçar atualização de toda a coleção vote_summaries
        try {
            // Buscar as partidas mais recentes (último mês)
            getCutoffDateForTimeFrame(TimeFrame.CURRENT_MONTH)

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
        } catch (_: Exception) {
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