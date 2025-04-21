package com.guicarneirodev.ltascore.data.datasource.local

import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.MatchState
import kotlinx.coroutines.flow.MutableStateFlow

class MatchLocalDataSource {
    private val matchesByLeague = mutableMapOf<String, MutableStateFlow<List<Match>>>()

    fun getMatches(leagueSlug: String): List<Match> {
        return matchesByLeague[leagueSlug]?.value ?: emptyList()
    }

    fun getMatchesByState(leagueSlug: String, state: MatchState): List<Match> {
        return getMatches(leagueSlug).filter { it.state == state }
    }

    fun getMatchById(matchId: String): Match? {
        return matchesByLeague.values
            .flatMap { it.value }
            .find { it.id == matchId }
    }

    fun getMatchesByBlock(leagueSlug: String, blockName: String): List<Match> {
        return getMatches(leagueSlug).filter { it.blockName == blockName }
    }

    fun saveMatches(matches: List<Match>?) {
        val matchesByLeagueMap = matches?.groupBy { it.leagueSlug }

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