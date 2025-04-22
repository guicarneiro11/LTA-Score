package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.repository.TeamFeedRepository
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.coroutines.flow.first

class ShareVoteToTeamFeedUseCase(
    private val teamFeedRepository: TeamFeedRepository,
    private val userRepository: UserRepository,
    private val voteRepository: VoteRepository
) {
    suspend operator fun invoke(matchId: String): Result<Unit> {
        try {
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            val favoriteTeamId = currentUser.favoriteTeamId
                ?: return Result.failure(Exception("Selecione um time favorito primeiro"))

            val votes = voteRepository.getUserVoteHistory(currentUser.id).first()
                .filter { it.matchId == matchId }

            if (votes.isEmpty()) {
                return Result.failure(Exception("Nenhum voto encontrado para esta partida"))
            }

            votes.forEach { vote ->
                teamFeedRepository.shareVoteToTeamFeed(
                    userId = currentUser.id,
                    teamId = favoriteTeamId,
                    vote = vote
                ).getOrThrow()
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}