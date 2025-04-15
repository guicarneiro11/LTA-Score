package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.repository.FriendshipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Caso de uso para obter o feed de votos dos amigos
 */
class GetFriendsFeedUseCase(
    private val friendshipRepository: FriendshipRepository
) {
    /**
     * Obtém o feed de votos dos amigos
     */
    fun execute(): Flow<List<FriendVoteHistoryItem>> {
        return friendshipRepository.getFriendsVoteHistory()
            .catch { exception ->
                // Log do erro mas emite lista vazia
                println("Erro ao obter feed de amigos: ${exception.message}")
                emit(emptyList())
            }
    }
}