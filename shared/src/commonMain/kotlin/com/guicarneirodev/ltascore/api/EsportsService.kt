package com.guicarneirodev.ltascore.api

import com.guicarneirodev.ltascore.api.models.LeaguesResponse
import com.guicarneirodev.ltascore.api.models.ScheduleResponse

interface EsportsService {
    suspend fun getLeagues(): LeaguesResponse
    suspend fun getSchedule(leagueSlug: String): ScheduleResponse
}