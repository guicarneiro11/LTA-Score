package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.ltascore.domain.repository.MatchPlayersRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseMatchPlayersRepository(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MatchPlayersRepository {

    private val matchPlayersCollection = firestore.collection("match_players")

    override suspend fun getParticipatingPlayers(matchId: String): Flow<List<String>> = callbackFlow {
        val listener = matchPlayersCollection.document(matchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val playerIds = snapshot.get("participatingPlayers") as? List<String> ?: emptyList()
                    trySend(playerIds)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun setParticipatingPlayers(matchId: String, playerIds: List<String>): Result<Unit> {
        return try {
            val data = hashMapOf("participatingPlayers" to playerIds)
            matchPlayersCollection.document(matchId).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}