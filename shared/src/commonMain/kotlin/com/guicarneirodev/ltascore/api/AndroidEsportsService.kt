package com.guicarneirodev.ltascore.api

import com.guicarneirodev.ltascore.api.models.LeaguesResponse
import com.guicarneirodev.ltascore.api.models.ScheduleResponse

class AndroidEsportsService : EsportsService {
    private val firebaseService = LoLEsportsFirebaseService()

    override suspend fun getLeagues(): LeaguesResponse {
        return firebaseService.getLeagues()
    }

    override suspend fun getSchedule(leagueSlug: String): ScheduleResponse {
        return firebaseService.getSchedule(leagueSlug)
    }
}