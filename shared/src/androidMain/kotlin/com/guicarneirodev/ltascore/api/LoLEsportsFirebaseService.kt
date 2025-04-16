package com.guicarneirodev.ltascore.api

import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.guicarneirodev.ltascore.api.models.LeaguesResponse
import com.guicarneirodev.ltascore.api.models.ScheduleResponse
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import com.google.gson.Gson

actual class LoLEsportsFirebaseService : LoLEsportsService {
    private val functions = Firebase.functions("us-central1")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val gson = Gson()

    actual override suspend fun getLeagues(language: String): LeaguesResponse {
        val data = hashMapOf(
            "language" to language
        )

        val result = functions
            .getHttpsCallable("getLeagues") // Mantendo getHttpsCallable em vez de httpsCallable
            .call(data)
            .await()

        val resultData = result.data // Acessando data como propriedade

        // Converter o resultado para string JSON
        val jsonString = convertToJsonString(resultData)
        return json.decodeFromString(jsonString)
    }

    actual override suspend fun getSchedule(leagueSlug: String, language: String): ScheduleResponse {
        // Convertendo slug para ID conforme necessário
        val leagueId = when (leagueSlug) {
            "lta_s" -> "113475181634818701" // LTA Sul
            "lta_n" -> "113475181634818702" // LTA Norte
            else -> throw IllegalArgumentException("Liga não suportada: $leagueSlug")
        }

        val data = hashMapOf(
            "language" to language,
            "leagueId" to leagueId
        )

        val result = functions
            .getHttpsCallable("getSchedule")
            .call(data)
            .await()

        val resultData = result.data

        // Converter o resultado para string JSON
        val jsonString = convertToJsonString(resultData)
        return json.decodeFromString(jsonString)
    }

    actual override suspend fun getMatch(matchId: String, language: String): ScheduleResponse {
        val data = hashMapOf(
            "language" to language,
            "id" to matchId
        )

        val result = functions
            .getHttpsCallable("getMatch")
            .call(data)
            .await()

        val resultData = result.data

        // Converter o resultado para string JSON
        val jsonString = convertToJsonString(resultData)
        return json.decodeFromString(jsonString)
    }

    // Função auxiliar para converter o resultado em string JSON
    private fun convertToJsonString(data: Any?): String {
        return when (data) {
            is String -> data
            is Map<*, *> -> gson.toJson(data)
            null -> "{}"
            else -> gson.toJson(data)
        }
    }
}