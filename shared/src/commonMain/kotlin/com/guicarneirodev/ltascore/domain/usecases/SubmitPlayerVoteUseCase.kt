package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.data.datasource.static.PlayersStaticDataSource
import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.Vote
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.first

/**
 * Caso de uso para enviar um voto para um jogador
 */
class SubmitPlayerVoteUseCase(
    private val voteRepository: VoteRepository,
    private val matchRepository: MatchRepository,
    private val playersDataSource: PlayersStaticDataSource
) {
    suspend operator fun invoke(
        matchId: String,
        playerId: String,
        userId: String,
        rating: Float
    ) {
        val roundedRating = (rating * 10).toInt() / 10f
        val validRating = roundedRating.coerceIn(0f, 10f)

        val vote = Vote(
            id = "${userId}_${matchId}_${playerId}",
            matchId = matchId,
            playerId = playerId,
            userId = userId,
            rating = validRating,
            timestamp = Clock.System.now()
        )

        // Submeter o voto principal
        voteRepository.submitVote(vote)

        // ADICIONAR: Criar e adicionar ao histórico
        try {
            // Obter detalhes da partida
            val match = matchRepository.getMatchById(matchId).first()

            // Obter dados do jogador
            val player = playersDataSource.getPlayerById(playerId)

            if (match != null && player != null) {
                // Identificar os times (jogador atual e oponente)
                val playerTeam = match.teams.find { team ->
                    team.players.any { it.id == playerId }
                }

                val opponentTeam = match.teams.find { it.id != playerTeam?.id }

                // Criar item de histórico
                val historyItem = UserVoteHistoryItem(
                    id = "${matchId}_${playerId}",
                    matchId = matchId,
                    matchDate = match.startTime,
                    playerId = playerId,
                    playerName = player.name,
                    playerNickname = player.nickname,
                    playerImage = player.imageUrl,
                    playerPosition = player.position,
                    teamId = player.teamId,
                    teamName = playerTeam?.name ?: "",
                    teamCode = playerTeam?.code ?: "",
                    teamImage = playerTeam?.imageUrl ?: "",
                    opponentTeamCode = opponentTeam?.code ?: "",
                    rating = validRating,
                    timestamp = Clock.System.now()
                )

                // Adicionar ao histórico do usuário
                voteRepository.addVoteToUserHistory(userId, historyItem)
            }
        } catch (e: Exception) {
            // Logar erro mas não interromper o fluxo principal
            println("Erro ao adicionar voto ao histórico: ${e.message}")
        }
    }
}