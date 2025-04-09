package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant

data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val profilePictureUrl: String? = null,
    val createdAt: Instant? = null
)