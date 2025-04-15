package com.guicarneirodev.ltascore.api

import com.guicarneirodev.ltascore.api.config.ApiConfig
import com.guicarneirodev.ltascore.api.models.LeaguesResponse
import com.guicarneirodev.ltascore.api.models.ScheduleResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class LoLEsportsApi {
    // Usar a chave da API do arquivo de configuração
    private val apiKey = ApiConfig.LOL_ESPORTS_API_KEY

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
            })
        }

        // Configurar o cabeçalho de autenticação para todas as requisições
        defaultRequest {
            header("x-api-key", apiKey)
        }
    }

    private val baseUrl = "https://esports-api.lolesports.com/persisted/gw"

    // Definindo uma função de log para depuração
    private val firebaseService = LoLEsportsFirebaseService()
    private val logger = LoLEsportsApiLogger()

    /**
     * Obtém informações sobre todas as ligas disponíveis
     */
    suspend fun getLeagues(): LeaguesResponse {
        logger.log("Buscando informações das ligas via Firebase")

        try {
            return firebaseService.getLeagues()
        } catch (e: Exception) {
            logger.log("Erro na chamada API getLeagues via Firebase: ${e.message}")
            throw e
        }
    }

    /**
     * Obtém o cronograma de partidas para uma liga específica
     *
     * @param leagueSlug O identificador da liga (ex: "lta_s" para LTA Sul)
     */
    suspend fun getSchedule(leagueSlug: String): ScheduleResponse {
        logger.log("Buscando calendário para liga: $leagueSlug via Firebase")

        try {
            return firebaseService.getSchedule(leagueSlug)
        } catch (e: Exception) {
            logger.log("Erro na chamada API via Firebase: ${e.message}")
            throw e
        }
    }

    suspend fun getMatch(matchId: String): ScheduleResponse {
        logger.log("Buscando detalhes para partida: $matchId")

        return httpClient.get("$baseUrl/getMatch") {
            parameter("hl", "pt-BR")
            parameter("id", matchId)
        }.body()
    }
}

class LoLEsportsApiLogger {
    fun log(message: String) {
        println("LoLEsportsApi - $message")
    }
}