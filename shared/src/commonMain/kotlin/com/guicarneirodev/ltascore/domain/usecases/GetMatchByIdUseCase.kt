package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Caso de uso para obter uma partida específica pelo ID
 */
class GetMatchByIdUseCase(
    private val matchRepository: MatchRepository
) {
    /**
     * Recupera os detalhes de uma partida pelo ID
     *
     * @param matchId O ID da partida a ser recuperada
     * @return Um Flow contendo a partida ou null caso não seja encontrada
     */
    suspend operator fun invoke(matchId: String): Flow<Match?> {
        return matchRepository.getMatchById(matchId)
            .catch { exception ->
                // Registra o erro e emite null
                println("Erro ao obter partida: ${exception.message}")
                emit(null)
            }
    }
}