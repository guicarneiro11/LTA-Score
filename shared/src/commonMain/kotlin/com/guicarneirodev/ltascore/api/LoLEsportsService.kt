package com.guicarneirodev.ltascore.api

import com.guicarneirodev.ltascore.api.models.LeaguesResponse
import com.guicarneirodev.ltascore.api.models.ScheduleResponse
import com.guicarneirodev.ltascore.domain.models.VodsResponse

interface LoLEsportsService {
    suspend fun getLeagues(language: String = "pt-BR"): LeaguesResponse
    suspend fun getSchedule(leagueSlug: String, language: String = "pt-BR"): ScheduleResponse
    suspend fun getMatch(matchId: String, language: String = "pt-BR"): ScheduleResponse
    suspend fun getVods(tournamentId: String = "113486838366247730", language: String = "pt-BR"): VodsResponse
}