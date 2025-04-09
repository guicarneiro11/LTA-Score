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

    override suspend fun getMatches(leagueSlug: String): Flow<List<Match>> {
        return flow {
            // Primeiro emite dados do cache
            emit(localDataSource.getMatches(leagueSlug))

            try {
                // Atualiza o cache
                refreshMatches(leagueSlug)

                // Emite dados atualizados
                emit(localDataSource.getMatches(leagueSlug))
            } catch (_: Exception) {
                // Em caso de erro, mantém os dados do cache
            }
        }
    }

    override suspend fun getMatchesByState(
        leagueSlug: String,
        state: MatchState
    ): Flow<List<Match>> {
        return flow {
            // Primeiro tenta obter do cache
            val cachedMatches = localDataSource.getMatchesByState(leagueSlug, state)
            emit(cachedMatches)

            // Se não houver dados em cache ou for solicitado explicitamente,
            // tenta atualizar os dados
            if (cachedMatches.isEmpty()) {
                try {
                    refreshMatches(leagueSlug)
                    emit(localDataSource.getMatchesByState(leagueSlug, state))
                } catch (_: Exception) {
                    // Em caso de erro, mantém os dados do cache
                }
            }
        }
    }

    override suspend fun getMatchById(matchId: String): Flow<Match?> {
        return flow {
            emit(localDataSource.getMatchById(matchId))
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
            // Chama a API
            val scheduleResponse = api.getSchedule(leagueSlug)

            // Converte a resposta da API para modelos de domínio
            val matches = scheduleResponse.data?.schedule?.events?.mapNotNull { event ->
                if (event.type != "match") return@mapNotNull null

                // Filtrar apenas partidas do Split 2 - verificando se a data é depois do início do Split 2
                // ou se o blockName está relacionado ao Split 2
                val matchDate = Instant.parse(event.startTime)
                val isSplit2 = matchDate > split2StartDate ||
                        isSplit2BlockName(event.blockName)

                // Pular partidas do Split 1
                if (!isSplit2) {
                    println("Ignorando partida do Split 1: ${event.blockName} em ${event.startTime}")
                    return@mapNotNull null
                }

                val matchDto = event.match
                val teams = matchDto.teams.map { teamDto ->
                    // Agora passamos tanto o ID (se disponível) quanto o código do time
                    val internalTeamId = TeamIdMapping.getInternalTeamId(teamDto.id, teamDto.code)

                    val players = playersDataSource.getPlayersByTeamId(internalTeamId)

                    Team(
                        id = teamDto.id ?: internalTeamId,
                        name = teamDto.name,
                        code = teamDto.code,
                        imageUrl = teamDto.image,
                        players = players,
                        result = parseTeamResult(teamDto.result.outcome, teamDto.result.gameWins,
                            teamDto.record?.wins ?: 0, teamDto.record?.losses ?: 0)
                    )
                }

                Match(
                    id = matchDto.id,
                    startTime = matchDate,
                    state = parseMatchState(event.state),
                    blockName = event.blockName,
                    leagueName = event.league.name,
                    leagueSlug = event.league.slug,
                    teams = teams,
                    bestOf = matchDto.strategy.count,
                    hasVod = matchDto.flags.contains("hasVod")
                )
            } ?: emptyList()

            // Salva no cache local
            localDataSource.saveMatches(matches)
        } catch (e: Exception) {
            println("Erro ao atualizar partidas: ${e.message}")
            throw e
        }
    }

    private fun isSplit2BlockName(blockName: String): Boolean {
        // Adapte esses critérios conforme a nomenclatura real usada pela API
        val split2Keywords = listOf("Semana", "Week", "Fase de Grupos", "Split 2")
        val split1Keywords = listOf("Eliminatórias", "Knockouts", "Split 1", "Playoffs")

        // Se contém palavras-chave do Split 2
        val containsSplit2Keyword = split2Keywords.any { blockName.contains(it, ignoreCase = true) }

        // Se contém palavras-chave do Split 1
        val containsSplit1Keyword = split1Keywords.any { blockName.contains(it, ignoreCase = true) }

        // Considera ser do Split 2 se tem palavra-chave do Split 2 e não tem do Split 1
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