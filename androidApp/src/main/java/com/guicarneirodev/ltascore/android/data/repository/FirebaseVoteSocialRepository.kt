package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.guicarneirodev.ltascore.domain.models.VoteComment
import com.guicarneirodev.ltascore.domain.models.VoteReaction
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteSocialRepository
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

class FirebaseVoteSocialRepository(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) : VoteSocialRepository {

    override suspend fun addReaction(voteId: String, reaction: String): Result<VoteReaction> {
        return try {
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            // Verificar se o usuário já tem uma reação para este voto
            val existingReactionQuery = firestore
                .collection("vote_reactions")
                .whereEqualTo("voteId", voteId)
                .whereEqualTo("userId", currentUser.id)
                .get()
                .await()

            // Se já existe, remover a reação anterior
            if (!existingReactionQuery.isEmpty) {
                val existingDoc = existingReactionQuery.documents.first()
                firestore.collection("vote_reactions")
                    .document(existingDoc.id)
                    .delete()
                    .await()
            }

            // Criar nova reação
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

            // Salvar no Firestore
            firestore.collection("vote_reactions")
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

    override suspend fun removeReaction(voteId: String): Result<Unit> {
        return try {
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            // Buscar a reação do usuário para este voto
            val reactionQuery = firestore
                .collection("vote_reactions")
                .whereEqualTo("voteId", voteId)
                .whereEqualTo("userId", currentUser.id)
                .get()
                .await()

            if (reactionQuery.isEmpty) {
                return Result.failure(Exception("Reação não encontrada"))
            }

            // Remover a reação
            val reactionDoc = reactionQuery.documents.first()
            firestore.collection("vote_reactions")
                .document(reactionDoc.id)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getReactionsForVote(voteId: String): Flow<List<VoteReaction>> = callbackFlow {
        val listener = firestore.collection("vote_reactions")
            .whereEqualTo("voteId", voteId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
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
                        null
                    }
                }

                trySend(reactions)
            }

        awaitClose {
            listener.remove()
        }
    }

    override fun getUserReactionForVote(voteId: String): Flow<VoteReaction?> = callbackFlow {
        try {
            val currentUser = userRepository.getCurrentUser().first()
            if (currentUser == null) {
                trySend(null)
                close()
                return@callbackFlow
            }

            val listener = firestore.collection("vote_reactions")
                .whereEqualTo("voteId", voteId)
                .whereEqualTo("userId", currentUser.id)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) {
                        trySend(null)
                        return@addSnapshotListener
                    }

                    val doc = snapshot.documents.firstOrNull()
                    if (doc == null) {
                        trySend(null)
                        return@addSnapshotListener
                    }

                    try {
                        val id = doc.getString("id") ?: return@addSnapshotListener
                        val userId = doc.getString("userId") ?: return@addSnapshotListener
                        val username = doc.getString("username") ?: ""
                        val reaction = doc.getString("reaction") ?: return@addSnapshotListener
                        val timestampDate = doc.getDate("timestamp")

                        val timestamp = if (timestampDate != null) {
                            Instant.fromEpochMilliseconds(timestampDate.time)
                        } else {
                            Clock.System.now()
                        }

                        val voteReaction = VoteReaction(
                            id = id,
                            voteId = voteId,
                            userId = userId,
                            username = username,
                            reaction = reaction,
                            timestamp = timestamp
                        )

                        trySend(voteReaction)
                    } catch (e: Exception) {
                        trySend(null)
                    }
                }

            awaitClose {
                listener.remove()
            }
        } catch (e: Exception) {
            trySend(null)
            close(e)
        }
    }

    override suspend fun addComment(voteId: String, text: String): Result<VoteComment> {
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

            // Salvar no Firestore
            firestore.collection("vote_comments")
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

    override suspend fun removeComment(commentId: String): Result<Unit> {
        return try {
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            // Verificar se o comentário pertence ao usuário
            val commentDoc = firestore.collection("vote_comments")
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

            // Remover o comentário
            firestore.collection("vote_comments")
                .document(commentId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCommentsForVote(voteId: String): Flow<List<VoteComment>> = callbackFlow {
        val listener = firestore.collection("vote_comments")
            .whereEqualTo("voteId", voteId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
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
                        null
                    }
                }

                trySend(comments)
            }

        awaitClose {
            listener.remove()
        }
    }
}