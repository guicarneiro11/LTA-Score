package com.guicarneirodev.ltascore.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: String,
    val name: String,
    val code: String,
    val imageUrl: String,
    val players: List<Player>,
    val result: TeamResult
)

@Serializable
data class TeamResult(
    val outcome: Outcome?,
    val gameWins: Int,
    val wins: Int,
    val losses: Int
)

enum class Outcome {
    WIN, LOSS
}