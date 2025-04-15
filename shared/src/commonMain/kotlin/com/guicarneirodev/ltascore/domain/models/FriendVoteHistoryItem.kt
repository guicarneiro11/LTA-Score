package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant

/**
 * Representa um item no histórico de votos de um amigo
 * Usamos composição em vez de herança para evitar conflitos com data class
 */
class FriendVoteHistoryItem(
    val baseVote: UserVoteHistoryItem,
    val friendId: String,
    val friendUsername: String
) {
    // Delegação para as propriedades de UserVoteHistoryItem para fácil acesso
    val id: String get() = baseVote.id
    val matchId: String get() = baseVote.matchId
    val matchDate: Instant get() = baseVote.matchDate
    val playerId: String get() = baseVote.playerId
    val playerName: String get() = baseVote.playerName
    val playerNickname: String get() = baseVote.playerNickname
    val playerImage: String get() = baseVote.playerImage
    val playerPosition: PlayerPosition get() = baseVote.playerPosition
    val teamId: String get() = baseVote.teamId
    val teamName: String get() = baseVote.teamName
    val teamCode: String get() = baseVote.teamCode
    val teamImage: String get() = baseVote.teamImage
    val opponentTeamCode: String get() = baseVote.opponentTeamCode
    val rating: Float get() = baseVote.rating
    val timestamp: Instant get() = baseVote.timestamp
}