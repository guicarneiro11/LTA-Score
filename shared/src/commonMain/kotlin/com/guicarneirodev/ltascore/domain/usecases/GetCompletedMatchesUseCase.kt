package com.guicarneirodev.ltascore.domain.usecases


import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.MatchState
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow

class GetCompletedMatchesUseCase(
    private val matchRepository: MatchRepository
) {
    suspend operator fun invoke(leagueSlug: String): Flow<List<Match>> {
        return matchRepository.getMatchesByState(leagueSlug, MatchState.COMPLETED)
    }
}