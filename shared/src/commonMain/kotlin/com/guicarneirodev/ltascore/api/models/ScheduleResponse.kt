package com.guicarneirodev.ltascore.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleResponse(
    val data: ScheduleData
)

@Serializable
data class ScheduleData(
    val schedule: Schedule
)

@Serializable
data class Schedule(
    val pages: Pages,
    val events: List<Event>
)

@Serializable
data class Pages(
    val older: String?,
    val newer: String?
)

@Serializable
data class Event(
    val startTime: String,
    val state: String,
    val type: String,
    val blockName: String,
    val league: League,
    val match: MatchDTO
)

@Serializable
data class League(
    val name: String,
    val slug: String
)

@Serializable
data class MatchDTO(
    val id: String,
    val flags: List<String>,
    val teams: List<TeamDTO>,
    val strategy: Strategy
)

@Serializable
data class TeamDTO(
    val id: String? = null,
    val name: String,
    val code: String,
    val image: String,
    val result: ResultDTO,
    val record: RecordDTO? = null
)

@Serializable
data class ResultDTO(
    val outcome: String? = null,
    val gameWins: Int
)

@Serializable
data class RecordDTO(
    val wins: Int,
    val losses: Int
)

@Serializable
data class Strategy(
    val type: String,
    val count: Int
)