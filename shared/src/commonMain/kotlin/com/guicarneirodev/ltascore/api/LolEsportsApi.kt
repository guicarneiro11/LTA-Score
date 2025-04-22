package com.guicarneirodev.ltascore.api

import com.guicarneirodev.ltascore.api.models.LeaguesResponse
import com.guicarneirodev.ltascore.api.models.ScheduleResponse
import com.guicarneirodev.ltascore.domain.models.VodsResponse

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

    suspend fun getVods(tournamentId: String = "113486838366247730"): VodsResponse {
        logger.log("Buscando VODs para torneio: $tournamentId")

        try {
            val response = service.getVods(tournamentId)
            logger.log("Resposta HTTP: Sucesso para VODs")
            return response
        } catch (e: Exception) {
            logger.log("Erro na chamada API getVods: ${e.message}")
            throw e
        }
    }
}

class LoLEsportsApiLogger {
    fun log(message: String) {
        println("LoLEsportsApi - $message")
    }
}