package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Representa um item no ranking de jogadores
 */
data class PlayerRankingItem(
    val player: Player,             // Informações do jogador
    val averageRating: Double,      // Média de avaliações
    val totalVotes: Int,            // Total de votos recebidos
    val teamId: String,             // ID do time atual
    val teamName: String,           // Nome do time atual
    val teamCode: String,           // Código/sigla do time
    val teamImage: String,          // Logo do time
    val position: PlayerPosition,   // Posição do jogador
    val lastMatchDate: Instant?     // Data da última partida avaliada
)

/**
 * Enumeração para os diferentes tipos de filtros de ranking
 */
enum class RankingFilter {
    ALL,            // Todos os jogadores
    BY_TEAM,        // Filtrar por time
    BY_POSITION,    // Filtrar por posição
    BY_WEEK,        // Melhores da semana
    BY_MONTH,       // Melhores do mês
    TOP_RATED,      // Melhor avaliados (todos os tempos)
    MOST_VOTED      // Mais votados (todos os tempos)
}

/**
 * Estado atual do filtro de ranking
 */
data class RankingFilterState(
    val currentFilter: RankingFilter = RankingFilter.ALL,
    val selectedTeamId: String? = null,
    val selectedPosition: PlayerPosition? = null,
    val selectedTimeFrame: TimeFrame = TimeFrame.ALL_TIME
)

/**
 * Período de tempo para filtrar
 */
enum class TimeFrame {
    CURRENT_WEEK,
    CURRENT_MONTH,
    ALL_TIME
}