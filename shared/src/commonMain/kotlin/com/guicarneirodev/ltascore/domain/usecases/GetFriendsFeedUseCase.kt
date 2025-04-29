package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.repository.FriendshipRepository
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class GetFriendsFeedUseCase(
    private val friendshipRepository: FriendshipRepository,
    private val voteRepository: VoteRepository,
    private val userRepository: UserRepository
) {
    fun execute(): Flow<List<FriendVoteHistoryItem>> {
        val friendsVotesFlow = friendshipRepository.getFriendsVoteHistory()
            .catch { exception ->
                println("Erro ao obter feed de amigos: ${exception.message}")
                emit(emptyList())
            }

        val userVotesFlow = flow {
            try {
                val currentUser = userRepository.getCurrentUser().firstOrNull()
                if (currentUser != null) {
                    val userVoteHistory = voteRepository.getUserVoteHistory(currentUser.id).firstOrNull() ?: emptyList()

                    val userVotesAsHistory = userVoteHistory.map { vote ->
                        FriendVoteHistoryItem(
                            baseVote = vote,
                            friendId = currentUser.id,
                            friendUsername = "${currentUser.username} (you)"
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

        return combine(friendsVotesFlow, userVotesFlow) { friendVotes, userVotes ->
            (friendVotes + userVotes).sortedByDescending { it.timestamp }
        }.catch { exception ->
            println("Erro ao combinar feeds: ${exception.message}")
            emit(emptyList())
        }
    }
}