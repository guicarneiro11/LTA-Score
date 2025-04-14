package com.guicarneirodev.ltascore.data.repository

import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.Vote
import com.guicarneirodev.ltascore.domain.models.VoteSummary
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * Implementação do repositório de votos
 *
 * Essa implementação inicial usa armazenamento em memória.
 * Para uma implementação completa, seria integrado com Firebase Firestore.
 */
class VoteRepositoryImpl : VoteRepository {

    private val votes = mutableListOf<Vote>()
    private val votesFlow = MutableStateFlow<List<Vote>>(emptyList())

    // NOVO: Armazenamento local temporário para o histórico de votos
    private val voteHistory = mutableMapOf<String, MutableList<UserVoteHistoryItem>>()
    private val voteHistoryFlow = MutableStateFlow<Map<String, List<UserVoteHistoryItem>>>(emptyMap())

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
        val historyItem = UserVoteHistoryItem(
            id = "${vote.matchId}_${vote.playerId}",
            matchId = vote.matchId,
            matchDate = Clock.System.now(), // Em uma implementação real, buscaria a data da partida
            playerId = vote.playerId,
            playerName = "Jogador", // Em uma implementação real, buscaria o nome do jogador
            playerNickname = "Nickname", // Em uma implementação real, buscaria o nickname
            playerImage = "", // Em uma implementação real, buscaria a imagem
            playerPosition = PlayerPosition.TOP, // Em uma implementação real, buscaria a posição
            teamId = "team1", // Em uma implementação real, buscaria o time
            teamName = "Time 1", // Em uma implementação real, buscaria o nome do time
            teamCode = "T1", // Em uma implementação real, buscaria o código do time
            teamImage = "", // Em uma implementação real, buscaria a imagem do time
            opponentTeamCode = "T2", // Em uma implementação real, buscaria o oponente
            rating = vote.rating,
            timestamp = vote.timestamp
        )

        // Adiciona ao histórico
        addVoteToUserHistory(vote.userId, historyItem)
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

    override suspend fun getUserVoteHistory(userId: String): Flow<List<UserVoteHistoryItem>> = flow {
        // Emite o histórico deste usuário ou uma lista vazia se não houver
        emit(voteHistory[userId] ?: emptyList())
    }

    override suspend fun addVoteToUserHistory(userId: String, historyItem: UserVoteHistoryItem) {
        // Cria a lista do usuário se não existir
        if (!voteHistory.containsKey(userId)) {
            voteHistory[userId] = mutableListOf()
        }

        // Verifica se já existe um item com este ID
        val existingIndex = voteHistory[userId]!!.indexOfFirst { it.id == historyItem.id }

        // Atualiza ou adiciona o item
        if (existingIndex >= 0) {
            voteHistory[userId]!![existingIndex] = historyItem
        } else {
            voteHistory[userId]!!.add(historyItem)
        }

        // Atualiza o fluxo de dados
        voteHistoryFlow.value = voteHistory.toMap()
    }
}