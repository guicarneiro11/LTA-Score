package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant

open class UserVoteHistoryItem(
    open val id: String,
    open val matchId: String,
    open val matchDate: Instant,
    open val playerId: String,
    open val playerName: String,
    open val playerNickname: String,
    open val playerImage: String,
    open val playerPosition: PlayerPosition,
    open val teamId: String,
    open val teamName: String,
    open val teamCode: String,
    open val teamImage: String,
    open val opponentTeamCode: String,
    open val rating: Float,
    open val timestamp: Instant
)