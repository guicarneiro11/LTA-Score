package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.FriendRequest
import com.guicarneirodev.ltascore.domain.models.Friendship
import com.guicarneirodev.ltascore.domain.repository.FriendshipRepository
import kotlinx.coroutines.flow.Flow

class ManageFriendshipsUseCase(
    private val friendshipRepository: FriendshipRepository
) {
    suspend fun addFriend(username: String): Result<Friendship> {
        return friendshipRepository.addFriendByUsername(username)
    }

    suspend fun removeFriend(friendId: String): Result<Unit> {
        return friendshipRepository.removeFriend(friendId)
    }

    fun getUserFriends(): Flow<List<Friendship>> {
        return friendshipRepository.getUserFriends()
    }

    suspend fun isFriend(userId: String): Flow<Boolean> {
        return friendshipRepository.isFriend(userId)
    }

    suspend fun sendFriendRequest(username: String): Result<FriendRequest> {
        return friendshipRepository.sendFriendRequest(username)
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Friendship> {
        return friendshipRepository.acceptFriendRequest(requestId)
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return friendshipRepository.rejectFriendRequest(requestId)
    }

    fun getPendingFriendRequests(): Flow<List<FriendRequest>> {
        return friendshipRepository.getPendingFriendRequests()
    }
}