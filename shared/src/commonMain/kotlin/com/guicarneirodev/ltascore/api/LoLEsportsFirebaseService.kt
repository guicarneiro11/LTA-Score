package com.guicarneirodev.ltascore.api

import com.guicarneirodev.ltascore.api.models.LeaguesResponse
import com.guicarneirodev.ltascore.api.models.ScheduleResponse

expect class LoLEsportsFirebaseService() : LoLEsportsService {
    override suspend fun getLeagues(language: String): LeaguesResponse
    override suspend fun getSchedule(leagueSlug: String, language: String): ScheduleResponse
    override suspend fun getMatch(matchId: String, language: String): ScheduleResponse
}