package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant

enum class FriendRequestStatus {
    PENDING, ACCEPTED, REJECTED
}

data class FriendRequest(
    val id: String,
    val senderId: String,
    val senderUsername: String,
    val receiverId: String,
    val status: FriendRequestStatus,
    val createdAt: Instant
)