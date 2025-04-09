package com.guicarneirodev.ltascore.api

import com.guicarneirodev.ltascore.api.config.ApiConfig
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
    private val logger = LoLEsportsApiLogger()

    /**
     * Obtém o cronograma de partidas para uma liga específica
     *
     * @param leagueSlug O identificador da liga (ex: "lta_s" para LTA Sul)
     */
    suspend fun getSchedule(leagueSlug: String): ScheduleResponse {
        // Mapeamento de slugs para IDs de liga
        val leagueId = when (leagueSlug) {
            "lta_s" -> "113475181634818701" // LTA Sul
            "lta_n" -> "113475181634818702" // LTA Norte (exemplo, verifique o ID correto)
            else -> throw IllegalArgumentException("Liga não suportada: $leagueSlug")
        }

        logger.log("Buscando calendário para liga: $leagueSlug (ID: $leagueId)")
        logger.log("Usando chave API: ${apiKey.take(5)}...")

        try {
            val response = httpClient.get("$baseUrl/getSchedule") {
                parameter("hl", "pt-BR")
                parameter("leagueId", leagueId)
            }

            logger.log("Resposta HTTP: ${response.status}")

            return response.body()
        } catch (e: Exception) {
            logger.log("Erro na chamada API: ${e.message}")
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