package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class GetMatchesUseCase(
    private val matchRepository: MatchRepository
) {
    suspend operator fun invoke(leagueSlug: String): Flow<List<Match>> {
        // Forçar uma atualização dos dados antes de retornar
        matchRepository.refreshMatches(leagueSlug)

        return matchRepository.getMatches(leagueSlug)
            .catch { exception ->
                println("Erro ao obter partidas: ${exception.message}")
                emit(emptyList())
            }
    }
}