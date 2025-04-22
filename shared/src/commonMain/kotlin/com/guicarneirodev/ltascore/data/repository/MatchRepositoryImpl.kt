package com.guicarneirodev.ltascore.data.repository

import com.guicarneirodev.ltascore.api.LoLEsportsApi
import com.guicarneirodev.ltascore.data.datasource.local.MatchLocalDataSource
import com.guicarneirodev.ltascore.data.datasource.static.PlayersStaticDataSource
import com.guicarneirodev.ltascore.data.datasource.static.TeamIdMapping
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.MatchState
import com.guicarneirodev.ltascore.domain.models.Outcome
import com.guicarneirodev.ltascore.domain.models.Team
import com.guicarneirodev.ltascore.domain.models.TeamResult
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant

class MatchRepositoryImpl(
    private val api: LoLEsportsApi,
    private val localDataSource: MatchLocalDataSource,
    private val playersDataSource: PlayersStaticDataSource
) : MatchRepository {

    private val split2StartDate = Instant.parse("2025-04-01T00:00:00Z")
    private val matchVods = mutableMapOf<String, String>()

    override suspend fun getMatches(leagueSlug: String): Flow<List<Match>> {
        return flow {
            emit(localDataSource.getMatches(leagueSlug))

            try {
                refreshMatches(leagueSlug)

                emit(localDataSource.getMatches(leagueSlug))
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun getMatchesByState(
        leagueSlug: String,
        state: MatchState
    ): Flow<List<Match>> {
        return flow {
            val cachedMatches = localDataSource.getMatchesByState(leagueSlug, state)
            emit(cachedMatches)

            if (cachedMatches.isEmpty()) {
                try {
                    refreshMatches(leagueSlug)
                    emit(localDataSource.getMatchesByState(leagueSlug, state))
                } catch (_: Exception) {
                }
            }
        }
    }

    override suspend fun getMatchById(matchId: String): Flow<Match?> {
        return flow {
            val match = localDataSource.getMatchById(matchId)

            if (match != null && match.teams.any { it.id == "isurus-estral" || it.code == "IE" }) {
                val updatedTeams = match.teams.map { team ->
                    if (team.id == "isurus-estral" || team.code == "IE") {
                        val filteredPlayers = playersDataSource.getPlayersByTeamIdAndDate(
                            "isurus-estral",
                            match.startTime,
                            match.blockName
                        )

                        team.copy(players = filteredPlayers)
                    } else {
                        team
                    }
                }

                emit(match.copy(teams = updatedTeams))
            } else {
                emit(match)
            }
        }
    }

    private fun ensureHttpsUrl(url: String): String {
        return if (url.startsWith("http://")) {
            url.replace("http://", "https://")
        } else {
            url
        }
    }

    override suspend fun getMatchesByBlock(
        leagueSlug: String,
        blockName: String
    ): Flow<List<Match>> {
        return flow {
            emit(localDataSource.getMatchesByBlock(leagueSlug, blockName))
        }
    }

    override suspend fun refreshMatches(leagueSlug: String) {
        try {

            loadVods()

            val scheduleResponse = api.getSchedule(leagueSlug)

            val matches = scheduleResponse.data?.schedule?.events?.mapNotNull { event ->
                if (event.type != "match") return@mapNotNull null

                val matchDate = Instant.parse(event.startTime)
                val isSplit2 = matchDate > split2StartDate ||
                        isSplit2BlockName(event.blockName)

                if (!isSplit2) {
                    println("Ignorando partida do Split 1: ${event.blockName} em ${event.startTime}")
                    return@mapNotNull null
                }

                val matchDto = event.match
                val matchId = matchDto.id

                val teams = matchDto.teams.map { teamDto ->
                    val internalTeamId = TeamIdMapping.getInternalTeamId(teamDto.id, teamDto.code)

                    val players = playersDataSource.getPlayersByTeamIdAndDate(
                        internalTeamId,
                        matchDate,
                        event.blockName
                    )

                    Team(
                        id = teamDto.id ?: internalTeamId,
                        name = teamDto.name,
                        code = teamDto.code,
                        imageUrl = ensureHttpsUrl(teamDto.image),
                        players = players,
                        result = parseTeamResult(teamDto.result.outcome, teamDto.result.gameWins,
                            teamDto.record?.wins ?: 0, teamDto.record?.losses ?: 0)
                    )
                }

                val vodUrl = matchVods[matchId]
                val hasVod = matchDto.flags.contains("hasVod") || vodUrl != null

                if (hasVod) {
                    println("🎬 Partida ${matchDto.id} tem VOD: $vodUrl")
                }

                Match(
                    id = matchId,
                    startTime = matchDate,
                    state = parseMatchState(event.state),
                    blockName = event.blockName,
                    leagueName = event.league.name,
                    leagueSlug = event.league.slug,
                    teams = teams,
                    bestOf = matchDto.strategy.count,
                    hasVod = hasVod,
                    vodUrl = vodUrl
                )
            } ?: emptyList()

            localDataSource.saveMatches(matches)

            val matchesWithVod = matches.filter { it.vodUrl != null }
            println("Total de partidas com VOD: ${matchesWithVod.size} de ${matches.size}")

        } catch (e: Exception) {
            println("Erro ao atualizar partidas: ${e.message}")
            throw e
        }
    }

    private fun isSplit2BlockName(blockName: String): Boolean {
        val split2Keywords = listOf("Semana", "Week", "Fase de Grupos", "Split 2")
        val split1Keywords = listOf("Eliminatórias", "Knockouts", "Split 1", "Playoffs")

        val containsSplit2Keyword = split2Keywords.any { blockName.contains(it, ignoreCase = true) }

        val containsSplit1Keyword = split1Keywords.any { blockName.contains(it, ignoreCase = true) }

        return containsSplit2Keyword && !containsSplit1Keyword
    }

    private fun parseMatchState(state: String): MatchState {
        return when (state.lowercase()) {
            "unstarted" -> MatchState.UNSTARTED
            "inprogress" -> MatchState.INPROGRESS
            "completed" -> MatchState.COMPLETED
            else -> MatchState.UNSTARTED
        }
    }

    private fun parseTeamResult(outcome: String?, gameWins: Int, wins: Int, losses: Int): TeamResult {
        val parsedOutcome = when (outcome?.lowercase()) {
            "win" -> Outcome.WIN
            "loss" -> Outcome.LOSS
            else -> null
        }

        return TeamResult(
            outcome = parsedOutcome,
            gameWins = gameWins,
            wins = wins,
            losses = losses
        )
    }

    private suspend fun loadVods() {
        try {
            println("Carregando VODs...")

            // Tente buscar os VODs da API
            val vodsResponse = api.getVods()
            var vodsFound = 0

            // Verificar se a resposta contém dados
            if (vodsResponse.data?.schedule?.events != null) {
                vodsResponse.data.schedule.events.forEach { event ->
                    // Verificar se a partida tem jogos e VODs
                    if (event.games.isNotEmpty() && event.games[0].vods.isNotEmpty()) {
                        val matchId = event.match.id
                        val vodParameter = event.games[0].vods[0].parameter
                        if (vodParameter.isNotEmpty()) {
                            val vodUrl = "https://www.youtube.com/watch?v=$vodParameter"
                            matchVods[matchId] = vodUrl
                            vodsFound++
                            println("⭐ VOD encontrado para partida $matchId: $vodUrl")
                        }
                    }
                }
            }

            println("Total de VODs carregados da API: $vodsFound")

            // Se nenhum VOD foi encontrado, carregue dados simulados para testes
            if (vodsFound == 0) {
                println("⚠️ Nenhum VOD encontrado na API, carregando dados simulados...")
                loadSimulatedVods()
            }
        } catch (e: Exception) {
            println("❌ Erro ao carregar VODs da API: ${e.message}")
            println("Carregando VODs simulados como fallback...")
            loadSimulatedVods()
        }
    }

    private fun loadSimulatedVods() {
        // Lista de partidas que terão VODs simulados
        val sampleMatchIds = listOf(
            "114103277165171981", // IE vs LEV
            "114103277165171985", // PAIN vs VKS
            "114103277165171989"  // LOUD vs FUR
        )

        // IDs de vídeos do YouTube para usar como exemplos
        val videoIds = listOf("_M8-bCz0AvM", "AgkBDvDBwRw", "2W8vIGVTUHI")

        sampleMatchIds.forEachIndexed { index, matchId ->
            val vodUrl = "https://www.youtube.com/watch?v=${videoIds[index % videoIds.size]}"
            matchVods[matchId] = vodUrl
            println("VOD simulado adicionado para partida $matchId: $vodUrl")
        }

        println("Total de VODs simulados: ${matchVods.size}")
    }
}