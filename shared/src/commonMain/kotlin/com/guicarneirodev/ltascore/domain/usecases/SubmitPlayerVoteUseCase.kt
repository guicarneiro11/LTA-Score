package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.Vote
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.datetime.Clock

/**
 * Caso de uso para enviar um voto para um jogador
 */
class SubmitPlayerVoteUseCase(
    private val voteRepository: VoteRepository
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
    }
}