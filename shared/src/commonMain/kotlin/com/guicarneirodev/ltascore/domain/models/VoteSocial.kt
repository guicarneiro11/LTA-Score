package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant

data class VoteReaction(
    val id: String,
    val voteId: String,
    val userId: String,
    val username: String,
    val reaction: String,
    val timestamp: Instant
)

data class VoteComment(
    val id: String,
    val voteId: String,
    val userId: String,
    val username: String,
    val text: String,
    val timestamp: Instant
)