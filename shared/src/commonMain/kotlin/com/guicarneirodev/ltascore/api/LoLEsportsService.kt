package com.guicarneirodev.ltascore.api

import com.guicarneirodev.ltascore.api.models.LeaguesResponse
import com.guicarneirodev.ltascore.api.models.ScheduleResponse

interface LoLEsportsService {
    suspend fun getLeagues(language: String = "en-US"): LeaguesResponse
    suspend fun getSchedule(leagueSlug: String, language: String = "en-US"): ScheduleResponse
    suspend fun getMatch(matchId: String, language: String = "en-US"): ScheduleResponse
}