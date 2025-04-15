package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.repository.FriendshipRepository
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

/**
 * Caso de uso para obter o feed de votos dos amigos
 */
class GetFriendsFeedUseCase(
    private val friendshipRepository: FriendshipRepository,
    private val voteRepository: VoteRepository,
    private val userRepository: UserRepository
) {
    /**
     * Obtém o feed combinado de votos dos amigos e do próprio usuário
     */
    fun execute(): Flow<List<FriendVoteHistoryItem>> {
        // Buscar votos dos amigos
        val friendsVotesFlow = friendshipRepository.getFriendsVoteHistory()
            .catch { exception ->
                println("Erro ao obter feed de amigos: ${exception.message}")
                emit(emptyList())
            }

        // Buscar votos do próprio usuário e convertê-los para o formato FriendVoteHistoryItem
        val userVotesFlow = flow {
            try {
                val currentUser = userRepository.getCurrentUser().firstOrNull()
                if (currentUser != null) {
                    val userVoteHistory = voteRepository.getUserVoteHistory(currentUser.id).firstOrNull() ?: emptyList()

                    // Converter UserVoteHistoryItem para FriendVoteHistoryItem
                    val userVotesAsHistory = userVoteHistory.map { vote ->
                        FriendVoteHistoryItem(
                            baseVote = vote,
                            friendId = currentUser.id,
                            friendUsername = "${currentUser.username} (você)" // Indicar que é o próprio usuário
                        )
                    }

                    emit(userVotesAsHistory)
                } else {
                    emit(emptyList())
                }
            } catch (e: Exception) {
                println("Erro ao obter votos do usuário atual: ${e.message}")
                emit(emptyList())
            }
        }

        // Combinar os dois fluxos e ordenar por data mais recente
        return combine(friendsVotesFlow, userVotesFlow) { friendVotes, userVotes ->
            (friendVotes + userVotes).sortedByDescending { it.timestamp }
        }.catch { exception ->
            println("Erro ao combinar feeds: ${exception.message}")
            emit(emptyList())
        }
    }
}

// Função auxiliar para obter o flow de votos do usuário atual
private suspend fun getUserVotes(
    userRepository: UserRepository,
    voteRepository: VoteRepository
): Flow<List<UserVoteHistoryItem>> = flow {
    try {
        val currentUser = userRepository.getCurrentUser().firstOrNull()
        if (currentUser != null) {
            voteRepository.getUserVoteHistory(currentUser.id).collect { votes ->
                emit(votes)
            }
        } else {
            emit(emptyList())
        }
    } catch (e: Exception) {
        println("Erro ao obter votos do usuário: ${e.message}")
        emit(emptyList())
    }
}