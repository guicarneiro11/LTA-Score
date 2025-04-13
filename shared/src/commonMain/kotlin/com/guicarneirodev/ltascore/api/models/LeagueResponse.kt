package com.guicarneirodev.ltascore.api.models

import kotlinx.serialization.Serializable

@Serializable
data class LeaguesResponse(
    val data: LeaguesData? = null,
    val errors: List<ApiError>? = null
)

@Serializable
data class LeaguesData(
    val leagues: List<LeagueInfo> = emptyList()
)

@Serializable
data class LeagueInfo(
    val id: String = "",
    val slug: String = "",
    val name: String = "",
    val region: String = "",
    val image: String = "",
    val priority: Int = 0,
    val displayPriority: DisplayPriority? = null
)

@Serializable
data class DisplayPriority(
    val position: Int = 0,
    val status: String = ""
)