package com.guicarneirodev.ltascore.domain.models

data class Player(
    val id: String,
    val name: String,
    val nickname: String,
    val imageUrl: String,
    val position: PlayerPosition,
    val teamId: String
)

enum class PlayerPosition {
    TOP, JUNGLE, MID, ADC, SUPPORT
}