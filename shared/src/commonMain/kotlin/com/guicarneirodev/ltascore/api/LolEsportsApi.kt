package com.guicarneirodev.ltascore.api

import com.guicarneirodev.ltascore.api.models.LeaguesResponse
import com.guicarneirodev.ltascore.api.models.ScheduleResponse

class LoLEsportsApi {
    private val service: LoLEsportsService = LoLEsportsFirebaseService()
    private val logger = LoLEsportsApiLogger()

    suspend fun getLeagues(): LeaguesResponse {
        logger.log("Buscando informações das ligas")

        try {
            val response = service.getLeagues()
            logger.log("Resposta recebida para ligas")
            return response
        } catch (e: Exception) {
            logger.log("Erro na chamada API getLeagues: ${e.message}")
            throw e
        }
    }

    suspend fun getSchedule(leagueSlug: String): ScheduleResponse {
        logger.log("Buscando calendário para liga: $leagueSlug")

        try {
            val response = service.getSchedule(leagueSlug)
            logger.log("Resposta HTTP: Sucesso")
            return response
        } catch (e: Exception) {
            logger.log("Erro na chamada API: ${e.message}")
            throw e
        }
    }
}

class LoLEsportsApiLogger {
    fun log(message: String) {
        println("LoLEsportsApi - $message")
    }
}