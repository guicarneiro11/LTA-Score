package com.guicarneirodev.ltascore.api

import com.guicarneirodev.ltascore.api.models.LeaguesResponse
import com.guicarneirodev.ltascore.api.models.ScheduleResponse

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class LoLEsportsFirebaseService actual constructor() : LoLEsportsService {
    actual override suspend fun getLeagues(language: String): LeaguesResponse {
        TODO("Not yet implemented")
    }

    actual override suspend fun getSchedule(
        leagueSlug: String,
        language: String
    ): ScheduleResponse {
        TODO("Not yet implemented")
    }

    actual override suspend fun getMatch(
        matchId: String,
        language: String
    ): ScheduleResponse {
        TODO("Not yet implemented")
    }
}