package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant

data class Friendship(
    val id: String,
    val userId: String,
    val friendId: String,
    val friendUsername: String,
    val createdAt: Instant
)