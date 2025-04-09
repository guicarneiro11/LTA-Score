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

                val matchDto = event.match
                val teams = matchDto.teams.map { teamDto ->
                    // Agora passamos tanto o ID (se disponível) quanto o código do time
                    val internalTeamId = TeamIdMapping.getInternalTeamId(teamDto.id, teamDto.code)

                    // Log para debug
                    println("Mapeando time: código=${teamDto.code}, ID API=${teamDto.id}, ID interno=${internalTeamId}")

                    val players = playersDataSource.getPlayersByTeamId(internalTeamId)

                    // Log de jogadores encontrados
                    println("Jogadores encontrados para ${teamDto.name}: ${players.size}")
                    players.forEach { player ->
                        println(" - ${player.nickname} (${player.position})")
                    }

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
                    startTime = Instant.parse(event.startTime),
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
            e.printStackTrace()
            throw e
        }
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