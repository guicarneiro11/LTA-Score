package com.guicarneirodev.ltascore.data.datasource.local

import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.MatchState
import kotlinx.coroutines.flow.MutableStateFlow

class MatchLocalDataSource {
    // Cache de partidas por liga
    private val matchesByLeague = mutableMapOf<String, MutableStateFlow<List<Match>>>()

    /**
     * Obtém todas as partidas de uma liga específica
     */
    fun getMatches(leagueSlug: String): List<Match> {
        return matchesByLeague[leagueSlug]?.value ?: emptyList()
    }

    /**
     * Obtém partidas por estado (concluídas, não iniciadas, etc.)
     */
    fun getMatchesByState(leagueSlug: String, state: MatchState): List<Match> {
        return getMatches(leagueSlug).filter { it.state == state }
    }

    /**
     * Obtém uma partida específica por ID
     */
    fun getMatchById(matchId: String): Match? {
        // Procura a partida em todos os caches de ligas
        return matchesByLeague.values
            .flatMap { it.value }
            .find { it.id == matchId }
    }

    /**
     * Obtém partidas por bloco/semana
     */
    fun getMatchesByBlock(leagueSlug: String, blockName: String): List<Match> {
        return getMatches(leagueSlug).filter { it.blockName == blockName }
    }

    /**
     * Salva partidas no cache
     */
    fun saveMatches(matches: List<Match>?) {
        // Agrupa partidas por liga
        val matchesByLeagueMap = matches?.groupBy { it.leagueSlug }

        // Atualiza ou cria o cache para cada liga
        matchesByLeagueMap?.forEach { (leagueSlug, leagueMatches) ->
            val existingFlow = matchesByLeague[leagueSlug]
            if (existingFlow != null) {
                existingFlow.value = leagueMatches
            } else {
                matchesByLeague[leagueSlug] = MutableStateFlow(leagueMatches)
            }
        }
    }

}