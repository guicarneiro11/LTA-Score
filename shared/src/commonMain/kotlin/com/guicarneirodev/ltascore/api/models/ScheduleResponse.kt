package com.guicarneirodev.ltascore.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleResponse(
    val data: ScheduleData? = null,
    val errors: List<ApiError>? = null
)

@Serializable
data class ApiError(
    val message: String? = null,
    val errorType: String? = null
)

@Serializable
data class ScheduleData(
    val schedule: Schedule? = null
)

@Serializable
data class Schedule(
    val pages: Pages? = null,
    val events: List<Event> = emptyList()
)

@Serializable
data class Pages(
    val older: String? = null,
    val newer: String? = null
)

@Serializable
data class Event(
    val startTime: String = "",
    val state: String = "",
    val type: String = "",
    val blockName: String = "",
    val league: League = League(),
    val match: MatchDTO = MatchDTO()
)

@Serializable
data class League(
    val name: String = "",
    val slug: String = ""
)

@Serializable
data class MatchDTO(
    val id: String = "",
    val flags: List<String> = emptyList(),
    val teams: List<TeamDTO> = emptyList(),
    val strategy: Strategy = Strategy()
)

@Serializable
data class TeamDTO(
    val id: String? = null,
    val name: String = "",
    val code: String = "",
    val image: String = "",
    val result: ResultDTO = ResultDTO(),
    val record: RecordDTO? = null
)

@Serializable
data class ResultDTO(
    val outcome: String? = null,
    val gameWins: Int = 0
)

@Serializable
data class RecordDTO(
    val wins: Int = 0,
    val losses: Int = 0
)

@Serializable
data class Strategy(
    val type: String = "",
    val count: Int = 0
)