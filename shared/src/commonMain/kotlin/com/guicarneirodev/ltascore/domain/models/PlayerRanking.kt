package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant


data class PlayerRankingItem(
    val player: Player,
    val averageRating: Double,
    val totalVotes: Int,
    val teamId: String,
    val teamName: String,
    val teamCode: String,
    val teamImage: String,
    val position: PlayerPosition,
    val lastMatchDate: Instant?
)

enum class RankingFilter {
    ALL,
    BY_TEAM,
    BY_POSITION,
    BY_WEEK,
    BY_MONTH,
    TOP_RATED,
    MOST_VOTED
}

data class RankingFilterState(
    val currentFilter: RankingFilter = RankingFilter.ALL,
    val selectedTeamId: String? = null,
    val selectedPosition: PlayerPosition? = null,
    val selectedTimeFrame: TimeFrame = TimeFrame.ALL_TIME
)

enum class TimeFrame {
    CURRENT_WEEK,
    CURRENT_MONTH,
    ALL_TIME
}