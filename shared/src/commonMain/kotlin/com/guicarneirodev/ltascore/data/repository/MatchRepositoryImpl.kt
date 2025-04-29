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

    private val leagueDateRanges = mapOf(
        "lta_s" to Pair(
            Instant.parse("2025-04-05T00:00:00Z"),
            Instant.parse("2025-06-15T23:59:59Z")
        ),
        "lta_n" to Pair(
            Instant.parse("2025-04-05T00:00:00Z"),
            Instant.parse("2025-06-16T23:59:59Z")
        ),
        "cd" to Pair(
            Instant.parse("2025-03-17T00:00:00Z"),
            Instant.parse("2025-06-09T23:59:59Z")
        )
    )

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
            val scheduleResponse = api.getSchedule(leagueSlug)

            val matches = scheduleResponse.data?.schedule?.events?.mapNotNull { event ->
                if (event.type != "match") return@mapNotNull null

                val matchDate = Instant.parse(event.startTime)

                val dateRange = leagueDateRanges[leagueSlug]
                val isInDateRange = if (dateRange != null) {
                    matchDate >= dateRange.first && matchDate <= dateRange.second
                } else {
                    val isSplit2 = leagueSlug != "cd" && (matchDate > Instant.parse("2025-04-01T00:00:00Z") ||
                            isSplit2BlockName(event.blockName))
                    val isSplit1CD = leagueSlug == "cd" && matchDate >= Instant.parse("2025-03-17T00:00:00Z")
                    isSplit2 || isSplit1CD
                }

                if (!isInDateRange) {
                    println("Ignorando partida fora do intervalo de datas: ${event.blockName} em ${event.startTime}")
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
}