package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.data.datasource.static.PlayersStaticDataSource
import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.Vote
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.first

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

        voteRepository.submitVote(vote)

        try {
            val match = matchRepository.getMatchById(matchId).first()

            val player = playersDataSource.getPlayerById(playerId)

            if (match != null && player != null) {
                val playerTeam = match.teams.find { team ->
                    team.players.any { it.id == playerId }
                }

                val opponentTeam = match.teams.find { it.id != playerTeam?.id }

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

                voteRepository.addVoteToUserHistory(userId, historyItem)
            }
        } catch (e: Exception) {

            println("Erro ao adicionar voto ao histórico: ${e.message}")
        }
    }
}