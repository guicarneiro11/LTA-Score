package com.guicarneirodev.ltascore.data.repository

import com.guicarneirodev.ltascore.domain.models.Vote
import com.guicarneirodev.ltascore.domain.models.VoteSummary
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Implementação do repositório de votos
 *
 * Essa implementação inicial usa armazenamento em memória.
 * Para uma implementação completa, seria integrado com Firebase Firestore.
 */
class VoteRepositoryImpl : VoteRepository {

    // Armazenamento local temporário de votos
    private val votes = mutableListOf<Vote>()
    private val votesFlow = MutableStateFlow<List<Vote>>(emptyList())

    override suspend fun submitVote(vote: Vote) {
        // Verifica se já existe um voto deste usuário para este jogador nesta partida
        val existingIndex = votes.indexOfFirst {
            it.userId == vote.userId &&
                    it.matchId == vote.matchId &&
                    it.playerId == vote.playerId
        }

        // Atualiza ou adiciona o voto
        if (existingIndex >= 0) {
            votes[existingIndex] = vote
        } else {
            votes.add(vote)
        }

        // Atualiza o fluxo de dados
        votesFlow.value = votes.toList()

        // Em uma implementação completa, aqui teríamos a integração com o Firebase
        // firestore.collection("votes").document(vote.id).set(vote)
    }

    override suspend fun getUserVotes(userId: String): Flow<List<Vote>> {
        return votesFlow.map { allVotes ->
            allVotes.filter { it.userId == userId }
        }
    }

    override suspend fun getUserVoteForPlayer(userId: String, matchId: String, playerId: String): Flow<Vote?> {
        return votesFlow.map { allVotes ->
            allVotes.find {
                it.userId == userId &&
                        it.matchId == matchId &&
                        it.playerId == playerId
            }
        }
    }

    override suspend fun getMatchVoteSummary(matchId: String): Flow<List<VoteSummary>> {
        return votesFlow.map { allVotes ->
            // Filtra votos para a partida específica
            val matchVotes = allVotes.filter { it.matchId == matchId }

            // Agrupa por jogador
            val votesByPlayer = matchVotes.groupBy { it.playerId }

            // Calcula médias e totais
            votesByPlayer.map { (playerId, playerVotes) ->
                val averageRating = playerVotes.map { it.rating }.average()
                VoteSummary(
                    playerId = playerId,
                    matchId = matchId,
                    averageRating = averageRating,
                    totalVotes = playerVotes.size
                )
            }
        }
    }

    override suspend fun hasUserVotedForMatch(userId: String, matchId: String): Flow<Boolean> {
        return votesFlow.map { allVotes ->
            allVotes.any { it.userId == userId && it.matchId == matchId }
        }
    }
}