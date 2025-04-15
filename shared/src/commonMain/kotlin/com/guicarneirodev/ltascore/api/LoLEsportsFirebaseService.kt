package com.guicarneirodev.ltascore.api

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.guicarneirodev.ltascore.api.models.LeaguesResponse
import com.guicarneirodev.ltascore.api.models.ScheduleResponse
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class LoLEsportsFirebaseService {
    private val functions: FirebaseFunctions = Firebase.functions
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private suspend fun <T> callFunction(functionName: String, data: Map<String, Any>): T {
        try {
            val result = functions.getHttpsCallable(functionName)
                .call(data)
                .await()
                .data.toString()

            return json.decodeFromString<T>(result)
        } catch (e: FirebaseFunctionsException) {
            throw Exception("Erro ao chamar função Firebase: ${e.message}")
        }
    }

    suspend fun getLeagues(): LeaguesResponse {
        return callFunction("getLeagues", mapOf("language" to "pt-BR"))
    }

    suspend fun getSchedule(leagueSlug: String): ScheduleResponse {
        // Mapeie o slug para o ID conforme necessário
        val leagueId = when (leagueSlug) {
            "lta_s" -> "113475181634818701" // LTA Sul
            "lta_n" -> "113475181634818702" // LTA Norte
            else -> throw IllegalArgumentException("Liga não suportada: $leagueSlug")
        }

        return callFunction("getSchedule", mapOf(
            "language" to "pt-BR",
            "leagueId" to leagueId
        ))
    }
}