package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant

data class Vote(
    val id: String,
    val matchId: String,
    val playerId: String,
    val userId: String,
    val rating: Float,
    val timestamp: Instant
)

data class VoteSummary(
    val playerId: String,
    val matchId: String,
    val averageRating: Double,
    val totalVotes: Int
)