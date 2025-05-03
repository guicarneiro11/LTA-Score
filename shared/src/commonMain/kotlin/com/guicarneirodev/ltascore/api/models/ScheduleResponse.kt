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
    val schedule: Schedule? = null,
    val event: EventDetail? = null
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
    val slug: String = "",
    val image: String = "",
    val id: String = ""
)

// Modelo para detalhes de um evento específico
@Serializable
data class EventDetail(
    val id: String = "",
    val type: String = "",
    val tournament: Tournament? = null,
    val league: League = League(),
    val match: MatchDetailDTO = MatchDetailDTO()
)

@Serializable
data class Tournament(
    val id: String = ""
)

@Serializable
data class MatchDetailDTO(
    val strategy: Strategy = Strategy(),
    val teams: List<TeamDTO> = emptyList(),
    val games: List<GameDTO> = emptyList()
)

@Serializable
data class GameDTO(
    val number: Int = 0,
    val id: String = "",
    val state: String = "",
    val teams: List<GameTeamDTO> = emptyList(),
    val vods: List<VodDTO> = emptyList()
)

@Serializable
data class GameTeamDTO(
    val id: String = "",
    val side: String = ""
)

@Serializable
data class VodDTO(
    val id: String = "",
    val parameter: String? = null,
    val locale: String = "",
    val mediaLocale: MediaLocaleDTO? = null,
    val provider: String = "",
    val offset: Int = 0,
    val firstFrameTime: String? = null,
    val startMillis: Long? = null,
    val endMillis: Long? = null
)

@Serializable
data class MediaLocaleDTO(
    val locale: String = "",
    val englishName: String = "",
    val translatedName: String = ""
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