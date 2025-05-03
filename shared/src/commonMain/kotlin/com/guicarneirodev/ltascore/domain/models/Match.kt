package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant

data class Match(
    val id: String,
    val startTime: Instant,
    val state: MatchState,
    val blockName: String,
    val leagueName: String,
    val leagueSlug: String,
    val teams: List<Team>,
    val bestOf: Int
)

enum class MatchState {
    UNSTARTED, INPROGRESS, COMPLETED
}