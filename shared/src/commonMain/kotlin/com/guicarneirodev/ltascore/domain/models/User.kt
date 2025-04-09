package com.guicarneirodev.ltascore.domain.models

data class User(
    val id: String,
    val username: String,
    val email: String?,
    val profileImageUrl: String?
)