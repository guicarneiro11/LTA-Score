package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.Friendship
import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.repository.FriendshipRepository
import kotlinx.coroutines.flow.Flow

/**
 * Caso de uso para gerenciar amizades
 */
class ManageFriendshipsUseCase(
    private val friendshipRepository: FriendshipRepository
) {
    /**
     * Adiciona um amigo usando o nome de usuário
     */
    suspend fun addFriend(username: String): Result<Friendship> {
        return friendshipRepository.addFriendByUsername(username)
    }

    /**
     * Remove um amigo
     */
    suspend fun removeFriend(friendId: String): Result<Unit> {
        return friendshipRepository.removeFriend(friendId)
    }

    /**
     * Obtém a lista de amigos do usuário atual
     */
    fun getUserFriends(): Flow<List<Friendship>> {
        return friendshipRepository.getUserFriends()
    }

    /**
     * Verifica se um usuário específico é amigo
     */
    suspend fun isFriend(userId: String): Flow<Boolean> {
        return friendshipRepository.isFriend(userId)
    }
}