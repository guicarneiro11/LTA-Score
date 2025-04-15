package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.Friendship
import kotlinx.coroutines.flow.Flow

/**
 * Interface do repositório responsável por gerenciar amizades
 */
interface FriendshipRepository {
    /**
     * Adiciona um amigo usando seu nome de usuário
     */
    suspend fun addFriendByUsername(username: String): Result<Friendship>

    /**
     * Remove um amigo
     */
    suspend fun removeFriend(friendId: String): Result<Unit>

    /**
     * Obtém a lista de amigos do usuário atual
     */
    fun getUserFriends(): Flow<List<Friendship>>

    /**
     * Verifica se um usuário específico é amigo
     */
    suspend fun isFriend(userId: String): Flow<Boolean>

    /**
     * Obtém o histórico de votos dos amigos
     */
    fun getFriendsVoteHistory(): Flow<List<FriendVoteHistoryItem>>
}