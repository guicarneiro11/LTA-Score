package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant

/**
 * Representa uma reação a um voto
 */
data class VoteReaction(
    val id: String,
    val voteId: String,
    val userId: String,
    val username: String,
    val reaction: String, // Emoji como "👍", "🔥", etc.
    val timestamp: Instant
)

/**
 * Representa um comentário em um voto
 */
data class VoteComment(
    val id: String,
    val voteId: String,
    val userId: String,
    val username: String,
    val text: String,
    val timestamp: Instant
)