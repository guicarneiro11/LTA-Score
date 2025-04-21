package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class GetMatchByIdUseCase(
    private val matchRepository: MatchRepository
) {
    suspend operator fun invoke(matchId: String): Flow<Match?> {
        return matchRepository.getMatchById(matchId)
            .catch { exception ->
                println("Erro ao obter partida: ${exception.message}")
                emit(null)
            }
    }
}