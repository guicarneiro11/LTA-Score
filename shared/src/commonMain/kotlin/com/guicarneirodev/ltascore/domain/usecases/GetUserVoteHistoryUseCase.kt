package com.guicarneirodev.ltascore.domain.usecases

import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first

class GetUserVoteHistoryUseCase(
    private val voteRepository: VoteRepository,
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<List<UserVoteHistoryItem>> = flow {
        try {
            val currentUser = userRepository.getCurrentUser().first()

            if (currentUser != null) {
                voteRepository.getUserVoteHistory(currentUser.id).collect { historyItems ->
                    emit(historyItems)
                }
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            println("Erro ao obter histórico de votos: ${e.message}")
            emit(emptyList())
        }
    }
}