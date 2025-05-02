package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant

data class MatchPrediction(
    val id: String,
    val matchId: String,
    val userId: String,
    val predictedTeamId: String,
    val timestamp: Instant
)

data class MatchPredictionStats(
    val matchId: String,
    val totalVotes: Int,
    val teamVotes: Map<String, Int>,
    val percentages: Map<String, Int>
)