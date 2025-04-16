package com.guicarneirodev.ltascore.api

import com.guicarneirodev.ltascore.api.models.LeaguesResponse
import com.guicarneirodev.ltascore.api.models.ScheduleResponse
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.functions
import kotlinx.serialization.json.Json

actual class LoLEsportsFirebaseService : LoLEsportsService {
    private val functions = Firebase.functions
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getLeagues(language: String): LeaguesResponse {
        val data = mapOf(
            "language" to language
        )

        val result = functions.httpsCallable("getLeagues").call(data).data as String
        return json.decodeFromString(result)
    }

    override suspend fun getSchedule(leagueSlug: String, language: String): ScheduleResponse {
        // Convertendo slug para ID conforme necessário
        val leagueId = when (leagueSlug) {
            "lta_s" -> "113475181634818701" // LTA Sul
            "lta_n" -> "113475181634818702" // LTA Norte
            else -> throw IllegalArgumentException("Liga não suportada: $leagueSlug")
        }

        val data = mapOf(
            "language" to language,
            "leagueId" to leagueId
        )

        val result = functions.httpsCallable("getSchedule").call(data).data as String
        return json.decodeFromString(result)
    }

    override suspend fun getMatch(matchId: String, language: String): ScheduleResponse {
        val data = mapOf(
            "language" to language,
            "id" to matchId
        )

        val result = functions.httpsCallable("getMatch").call(data).data as String
        return json.decodeFromString(result)
    }
}