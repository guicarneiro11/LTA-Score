package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant

data class UserVoteHistoryItem(
    val id: String,
    val matchId: String,
    val matchDate: Instant,
    val playerId: String,
    val playerName: String,
    val playerNickname: String,
    val playerImage: String,
    val playerPosition: PlayerPosition,
    val teamId: String,
    val teamName: String,
    val teamCode: String,
    val teamImage: String,
    val opponentTeamCode: String,
    val rating: Float,
    val timestamp: Instant
)