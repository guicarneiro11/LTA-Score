package com.guicarneirodev.ltascore.domain.models

import com.guicarneirodev.ltascore.api.models.ApiError
import com.guicarneirodev.ltascore.api.models.ResultDTO
import com.guicarneirodev.ltascore.api.models.Strategy
import kotlinx.serialization.Serializable

@Serializable
data class VodsResponse(
    val data: VodsData? = null,
    val errors: List<ApiError>? = null
)

@Serializable
data class VodsData(
    val schedule: VodsSchedule? = null,
    val nextUnstartedMatch: NextUnstartedMatch? = null
)

@Serializable
data class VodsSchedule(
    val events: List<VodEvent> = emptyList()
)

@Serializable
data class NextUnstartedMatch(
    val events: List<NextEvent> = emptyList()
)

@Serializable
data class NextEvent(
    val startTime: String = ""
)

@Serializable
data class VodEvent(
    val startTime: String = "",
    val state: String = "",
    val blockName: String = "",
    val league: LeagueInfo = LeagueInfo(),
    val match: VodMatch = VodMatch(),
    val games: List<Game> = emptyList()
)

@Serializable
data class LeagueInfo(
    val name: String = ""
)

@Serializable
data class VodMatch(
    val id: String = "",
    val type: String = "",
    val teams: List<VodTeam> = emptyList(),
    val strategy: Strategy = Strategy()
)

@Serializable
data class VodTeam(
    val name: String = "",
    val code: String = "",
    val image: String = "",
    val result: ResultDTO = ResultDTO()
)

@Serializable
data class Game(
    val id: String = "",
    val state: String = "",
    val vods: List<Vod> = emptyList()
)

@Serializable
data class Vod(
    val parameter: String = "",
    val startMillis: Long? = null,
    val endMillis: Long? = null
)