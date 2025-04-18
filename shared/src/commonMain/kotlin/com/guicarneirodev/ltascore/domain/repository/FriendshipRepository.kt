package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.FriendRequest
import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.Friendship
import kotlinx.coroutines.flow.Flow

interface FriendshipRepository {
    suspend fun addFriendByUsername(username: String): Result<Friendship>
    suspend fun removeFriend(friendId: String): Result<Unit>
    fun getUserFriends(): Flow<List<Friendship>>
    suspend fun isFriend(userId: String): Flow<Boolean>
    fun getFriendsVoteHistory(): Flow<List<FriendVoteHistoryItem>>

    suspend fun sendFriendRequest(username: String): Result<FriendRequest>
    suspend fun acceptFriendRequest(requestId: String): Result<Friendship>
    suspend fun rejectFriendRequest(requestId: String): Result<Unit>
    fun getPendingFriendRequests(): Flow<List<FriendRequest>>
}