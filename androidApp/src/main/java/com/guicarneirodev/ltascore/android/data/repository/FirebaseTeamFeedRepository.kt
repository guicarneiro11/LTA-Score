package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.TeamFeedItem
import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.VoteComment
import com.guicarneirodev.ltascore.domain.models.VoteReaction
import com.guicarneirodev.ltascore.domain.repository.TeamFeedRepository
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.Date
import java.util.UUID

class FirebaseTeamFeedRepository(
    firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) : TeamFeedRepository {

    private val teamFeedsCollection = firestore.collection("team_feeds")
    private val reactionsCollection = firestore.collection("team_vote_reactions")
    private val commentsCollection = firestore.collection("team_vote_comments")

    override fun getTeamFeed(teamId: String): Flow<List<TeamFeedItem>> = callbackFlow {
        try {
            println("Buscando feed para o time: $teamId")

            val listener = teamFeedsCollection
                .document(teamId)
                .collection("votes")
                .orderBy("sharedAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("Erro ao buscar feed da torcida: ${error.message}")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        println("Feed da torcida vazio para o time $teamId")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val feedItems = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val userId = doc.getString("userId") ?: return@mapNotNull null
                            val username = doc.getString("username") ?: ""
                            val matchId = doc.getString("matchId") ?: return@mapNotNull null
                            val matchDateTimestamp = doc.getDate("matchDate")
                            val playerId = doc.getString("playerId") ?: return@mapNotNull null
                            val playerName = doc.getString("playerName") ?: ""
                            val playerNickname = doc.getString("playerNickname") ?: ""
                            val playerImage = doc.getString("playerImage") ?: ""
                            val playerPositionStr = doc.getString("playerPosition") ?: "TOP"
                            val teamIdValue = doc.getString("teamId") ?: ""
                            val teamName = doc.getString("teamName") ?: ""
                            val teamCode = doc.getString("teamCode") ?: ""
                            val teamImage = doc.getString("teamImage") ?: ""
                            val opponentTeamCode = doc.getString("opponentTeamCode") ?: ""
                            val rating = doc.getDouble("rating")?.toFloat() ?: 0f
                            val timestampDate = doc.getDate("timestamp")
                            val sharedAtDate = doc.getDate("sharedAt")

                            val playerPosition = try {
                                PlayerPosition.valueOf(playerPositionStr)
                            } catch (_: Exception) {
                                PlayerPosition.TOP
                            }

                            val matchDate = if (matchDateTimestamp != null) {
                                Instant.fromEpochMilliseconds(matchDateTimestamp.time)
                            } else {
                                Clock.System.now()
                            }

                            val timestamp = if (timestampDate != null) {
                                Instant.fromEpochMilliseconds(timestampDate.time)
                            } else {
                                Clock.System.now()
                            }

                            val sharedAt = if (sharedAtDate != null) {
                                Instant.fromEpochMilliseconds(sharedAtDate.time)
                            } else {
                                Clock.System.now()
                            }

                            TeamFeedItem(
                                id = id,
                                userId = userId,
                                username = username,
                                matchId = matchId,
                                matchDate = matchDate,
                                playerId = playerId,
                                playerName = playerName,
                                playerNickname = playerNickname,
                                playerImage = playerImage,
                                playerPosition = playerPosition,
                                teamId = teamIdValue,
                                teamName = teamName,
                                teamCode = teamCode,
                                teamImage = teamImage,
                                opponentTeamCode = opponentTeamCode,
                                rating = rating,
                                timestamp = timestamp,
                                sharedAt = sharedAt
                            )
                        } catch (e: Exception) {
                            println("Erro ao processar item do feed: ${e.message}")
                            null
                        }
                    }

                    println("Encontrados ${feedItems.size} itens no feed da torcida $teamId")
                    trySend(feedItems)
                }

            awaitClose {
                listener.remove()
            }
        } catch (e: Exception) {
            println("Erro crítico ao buscar feed da torcida: ${e.message}")
            e.printStackTrace()
            trySend(emptyList())
            close(e)
        }
    }

    override suspend fun shareVoteToTeamFeed(
        userId: String,
        teamId: String,
        vote: UserVoteHistoryItem
    ): Result<Unit> {
        return try {
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            val sharedAt = Clock.System.now()
            val voteId = "${vote.matchId}_${vote.playerId}_${userId}_${sharedAt.toEpochMilliseconds()}"

            val teamFeedItem = hashMapOf(
                "id" to voteId,
                "userId" to userId,
                "username" to currentUser.username,
                "matchId" to vote.matchId,
                "matchDate" to Date.from(vote.matchDate.toJavaInstant()),
                "playerId" to vote.playerId,
                "playerName" to vote.playerName,
                "playerNickname" to vote.playerNickname,
                "playerImage" to vote.playerImage,
                "playerPosition" to vote.playerPosition.name,
                "teamId" to vote.teamId,
                "teamName" to vote.teamName,
                "teamCode" to vote.teamCode,
                "teamImage" to vote.teamImage,
                "opponentTeamCode" to vote.opponentTeamCode,
                "rating" to vote.rating,
                "timestamp" to Date.from(vote.timestamp.toJavaInstant()),
                "sharedAt" to Date.from(sharedAt.toJavaInstant())
            )

            teamFeedsCollection
                .document(teamId)
                .collection("votes")
                .document(voteId)
                .set(teamFeedItem)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            println("Erro ao compartilhar voto para o feed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getReactionsForTeamVote(voteId: String): Flow<List<VoteReaction>> = callbackFlow {
        val listener = reactionsCollection
            .whereEqualTo("voteId", voteId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Erro ao buscar reações: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    println("Nenhuma reação encontrada para o voto $voteId")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val reactions = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: return@mapNotNull null
                        val userId = doc.getString("userId") ?: return@mapNotNull null
                        val username = doc.getString("username") ?: ""
                        val reaction = doc.getString("reaction") ?: return@mapNotNull null
                        val timestampDate = doc.getDate("timestamp")

                        val timestamp = if (timestampDate != null) {
                            Instant.fromEpochMilliseconds(timestampDate.time)
                        } else {
                            Clock.System.now()
                        }

                        VoteReaction(
                            id = id,
                            voteId = voteId,
                            userId = userId,
                            username = username,
                            reaction = reaction,
                            timestamp = timestamp
                        )
                    } catch (e: Exception) {
                        println("Erro ao processar reação: ${e.message}")
                        null
                    }
                }

                trySend(reactions)
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun getCommentsForTeamVote(voteId: String): Flow<List<VoteComment>> = callbackFlow {
        val listener = commentsCollection
            .whereEqualTo("voteId", voteId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Erro ao buscar comentários: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    println("Nenhum comentário encontrado para o voto $voteId")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val comments = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: return@mapNotNull null
                        val userId = doc.getString("userId") ?: return@mapNotNull null
                        val username = doc.getString("username") ?: ""
                        val text = doc.getString("text") ?: return@mapNotNull null
                        val timestampDate = doc.getDate("timestamp")

                        val timestamp = if (timestampDate != null) {
                            Instant.fromEpochMilliseconds(timestampDate.time)
                        } else {
                            Clock.System.now()
                        }

                        VoteComment(
                            id = id,
                            voteId = voteId,
                            userId = userId,
                            username = username,
                            text = text,
                            timestamp = timestamp
                        )
                    } catch (e: Exception) {
                        println("Erro ao processar comentário: ${e.message}")
                        null
                    }
                }

                trySend(comments)
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun addReactionToTeamVote(voteId: String, reaction: String): Result<VoteReaction> {
        return try {
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            val existingReactionQuery = reactionsCollection
                .whereEqualTo("voteId", voteId)
                .whereEqualTo("userId", currentUser.id)
                .get()
                .await()

            if (!existingReactionQuery.isEmpty) {
                val existingDoc = existingReactionQuery.documents.first()
                reactionsCollection
                    .document(existingDoc.id)
                    .delete()
                    .await()
            }

            val reactionId = UUID.randomUUID().toString()
            val timestamp = Clock.System.now()

            val voteReaction = VoteReaction(
                id = reactionId,
                voteId = voteId,
                userId = currentUser.id,
                username = currentUser.username,
                reaction = reaction,
                timestamp = timestamp
            )

            reactionsCollection
                .document(reactionId)
                .set(mapOf(
                    "id" to voteReaction.id,
                    "voteId" to voteReaction.voteId,
                    "userId" to voteReaction.userId,
                    "username" to voteReaction.username,
                    "reaction" to voteReaction.reaction,
                    "timestamp" to Date.from(timestamp.toJavaInstant())
                ))
                .await()

            Result.success(voteReaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeReactionFromTeamVote(voteId: String): Result<Unit> {
        return try {
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            val reactionQuery = reactionsCollection
                .whereEqualTo("voteId", voteId)
                .whereEqualTo("userId", currentUser.id)
                .get()
                .await()

            if (reactionQuery.isEmpty) {
                return Result.failure(Exception("Reação não encontrada"))
            }

            val reactionDoc = reactionQuery.documents.first()
            reactionsCollection
                .document(reactionDoc.id)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addCommentToTeamVote(voteId: String, text: String): Result<VoteComment> {
        return try {
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            val commentId = UUID.randomUUID().toString()
            val timestamp = Clock.System.now()

            val comment = VoteComment(
                id = commentId,
                voteId = voteId,
                userId = currentUser.id,
                username = currentUser.username,
                text = text,
                timestamp = timestamp
            )

            commentsCollection
                .document(commentId)
                .set(mapOf(
                    "id" to comment.id,
                    "voteId" to comment.voteId,
                    "userId" to comment.userId,
                    "username" to comment.username,
                    "text" to comment.text,
                    "timestamp" to Date.from(timestamp.toJavaInstant())
                ))
                .await()

            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeCommentFromTeamVote(commentId: String): Result<Unit> {
        return try {
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            val commentDoc = commentsCollection
                .document(commentId)
                .get()
                .await()

            if (!commentDoc.exists()) {
                return Result.failure(Exception("Comentário não encontrado"))
            }

            val commentUserId = commentDoc.getString("userId")
            if (commentUserId != currentUser.id) {
                return Result.failure(Exception("Você não pode remover comentários de outros usuários"))
            }

            commentsCollection
                .document(commentId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}