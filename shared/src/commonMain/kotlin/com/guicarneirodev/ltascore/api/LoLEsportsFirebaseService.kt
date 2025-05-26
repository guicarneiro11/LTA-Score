package com.guicarneirodev.ltascore.api

import com.guicarneirodev.ltascore.api.models.LeaguesResponse
import com.guicarneirodev.ltascore.api.models.ScheduleResponse

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class LoLEsportsFirebaseService() : LoLEsportsService {
    override suspend fun getLeagues(language: String): LeaguesResponse
    override suspend fun getSchedule(leagueSlug: String, language: String): ScheduleResponse
    override suspend fun getMatch(matchId: String, language: String): ScheduleResponse
}