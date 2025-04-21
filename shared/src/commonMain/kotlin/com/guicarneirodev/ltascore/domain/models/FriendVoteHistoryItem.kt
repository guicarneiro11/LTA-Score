package com.guicarneirodev.ltascore.domain.models

import kotlinx.datetime.Instant

class FriendVoteHistoryItem(
    val baseVote: UserVoteHistoryItem,
    val friendId: String,
    val friendUsername: String
) {
    val id: String get() = baseVote.id
    val matchId: String get() = baseVote.matchId
    val matchDate: Instant get() = baseVote.matchDate
    val playerId: String get() = baseVote.playerId
    val playerNickname: String get() = baseVote.playerNickname
    val playerImage: String get() = baseVote.playerImage
    val playerPosition: PlayerPosition get() = baseVote.playerPosition
    val teamId: String get() = baseVote.teamId
    val teamCode: String get() = baseVote.teamCode
    val opponentTeamCode: String get() = baseVote.opponentTeamCode
    val rating: Float get() = baseVote.rating
    val timestamp: Instant get() = baseVote.timestamp
}