package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.guicarneirodev.ltascore.domain.models.FriendRequest
import com.guicarneirodev.ltascore.domain.models.FriendRequestStatus
import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.Friendship
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import com.guicarneirodev.ltascore.domain.repository.FriendshipRepository
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

class FirebaseFriendshipRepository(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) : FriendshipRepository {

    private val friendsCollection = "user_friends"
    private val requestsCollection = "friend_requests"

    override suspend fun addFriendByUsername(username: String): Result<Friendship> {
        return try {
            // Obter o usuário atual
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            // Verificar se não está tentando adicionar a si mesmo
            if (currentUser.username == username) {
                return Result.failure(Exception("Você não pode adicionar a si mesmo como amigo"))
            }

            // Buscar o usuário pelo nome de usuário
            val friendSnapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (friendSnapshot.isEmpty) {
                return Result.failure(Exception("Usuário não encontrado"))
            }

            val friendDoc = friendSnapshot.documents.first()
            val friendId = friendDoc.id
            val friendUsername = friendDoc.getString("username") ?: ""

            // Verificar se já é amigo
            val existingFriendship = firestore.collection(friendsCollection)
                .document(currentUser.id)
                .collection("friends")
                .document(friendId)
                .get()
                .await()

            if (existingFriendship.exists()) {
                return Result.failure(Exception("Usuário já é seu amigo"))
            }

            // Criar a amizade
            val friendship = Friendship(
                id = "${currentUser.id}_${friendId}",
                userId = currentUser.id,
                friendId = friendId,
                friendUsername = friendUsername,
                createdAt = Clock.System.now()
            )

            // Salvar nos dois lados (usuário -> amigo e amigo -> usuário)
            firestore.collection(friendsCollection)
                .document(currentUser.id)
                .collection("friends")
                .document(friendId)
                .set(
                    mapOf(
                        "id" to friendship.id,
                        "userId" to friendship.userId,
                        "friendId" to friendship.friendId,
                        "friendUsername" to friendship.friendUsername,
                        "createdAt" to Date.from(friendship.createdAt.toJavaInstant())
                    )
                )
                .await()

            // Versão espelhada da amizade (para o outro usuário)
            val reverseFriendship = Friendship(
                id = "${friendId}_${currentUser.id}",
                userId = friendId,
                friendId = currentUser.id,
                friendUsername = currentUser.username,
                createdAt = Clock.System.now()
            )

            firestore.collection(friendsCollection)
                .document(friendId)
                .collection("friends")
                .document(currentUser.id)
                .set(
                    mapOf(
                        "id" to reverseFriendship.id,
                        "userId" to reverseFriendship.userId,
                        "friendId" to reverseFriendship.friendId,
                        "friendUsername" to reverseFriendship.friendUsername,
                        "createdAt" to Date.from(reverseFriendship.createdAt.toJavaInstant())
                    )
                )
                .await()

            Result.success(friendship)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFriend(friendId: String): Result<Unit> {
        return try {
            // Obter o usuário atual
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            // Remover dos dois lados
            firestore.collection(friendsCollection)
                .document(currentUser.id)
                .collection("friends")
                .document(friendId)
                .delete()
                .await()

            firestore.collection(friendsCollection)
                .document(friendId)
                .collection("friends")
                .document(currentUser.id)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getUserFriends(): Flow<List<Friendship>> = callbackFlow {
        // Obter o usuário atual
        try {
            val currentUser = userRepository.getCurrentUser().first()
            if (currentUser == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            // Configurar o listener para a coleção de amigos
            val listener = firestore.collection(friendsCollection)
                .document(currentUser.id)
                .collection("friends")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val friends = snapshot.documents.mapNotNull { doc ->
                        try {
                            val friendId = doc.getString("friendId") ?: return@mapNotNull null
                            val friendUsername = doc.getString("friendUsername") ?: ""
                            val createdAtTimestamp = doc.getDate("createdAt")

                            val createdAt = if (createdAtTimestamp != null) {
                                Instant.fromEpochMilliseconds(createdAtTimestamp.time)
                            } else {
                                Clock.System.now()
                            }

                            Friendship(
                                id = "${currentUser.id}_${friendId}",
                                userId = currentUser.id,
                                friendId = friendId,
                                friendUsername = friendUsername,
                                createdAt = createdAt
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }

                    trySend(friends)
                }

            awaitClose {
                listener.remove()
            }
        } catch (e: Exception) {
            trySend(emptyList())
            close(e)
        }
    }

    override suspend fun isFriend(userId: String): Flow<Boolean> = callbackFlow {
        try {
            val currentUser = userRepository.getCurrentUser().first()
            if (currentUser == null) {
                trySend(false)
                close()
                return@callbackFlow
            }

            val listener = firestore.collection(friendsCollection)
                .document(currentUser.id)
                .collection("friends")
                .document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(false)
                        return@addSnapshotListener
                    }

                    trySend(snapshot != null && snapshot.exists())
                }

            awaitClose {
                listener.remove()
            }
        } catch (e: Exception) {
            trySend(false)
            close(e)
        }
    }

    override fun getFriendsVoteHistory(): Flow<List<FriendVoteHistoryItem>> = callbackFlow {
        try {
            val currentUser = userRepository.getCurrentUser().first()
            if (currentUser == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            println("Buscando histórico de votos dos amigos para usuário: ${currentUser.id}")

            // Lista para armazenar todos os listeners que criaremos
            val listeners = mutableListOf<ListenerRegistration>()

            // Mapa para manter o estado atual dos votos dos amigos
            val allFriendsVotes = mutableMapOf<String, List<FriendVoteHistoryItem>>()

            // Função auxiliar para atualizar o feed combinado
            // IMPORTANTE: Declarar a função antes de usá-la!
            fun updateFeed() {
                val combinedFeed = allFriendsVotes.values.flatten()
                    .sortedByDescending { it.timestamp }
                trySend(combinedFeed)
            }

            // Listener principal para a lista de amigos
            val friendsListener = firestore.collection("user_friends")
                .document(currentUser.id)
                .collection("friends")
                .addSnapshotListener { friendsSnapshot, friendsError ->
                    if (friendsError != null) {
                        println("Erro ao observar lista de amigos: ${friendsError.message}")
                        return@addSnapshotListener
                    }

                    if (friendsSnapshot == null || friendsSnapshot.isEmpty) {
                        println("Nenhum amigo encontrado")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    // Remover listeners antigos quando a lista de amigos muda
                    listeners.forEach { it.remove() }
                    listeners.clear()
                    allFriendsVotes.clear()

                    // Para cada amigo, criar um listener para seu histórico de votos
                    friendsSnapshot.documents.forEach { friendDoc ->
                        val friendId = friendDoc.getString("friendId") ?: return@forEach
                        val friendUsername = friendDoc.getString("friendUsername") ?: "Amigo"

                        println("Configurando listener para votos do amigo: $friendUsername")

                        // Listener para o histórico de votos deste amigo
                        val votesListener = firestore.collection("user_vote_history")
                            .document(friendId)
                            .collection("votes")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(15) // Limitando para performance
                            .addSnapshotListener { votesSnapshot, votesError ->
                                if (votesError != null) {
                                    println("Erro ao observar votos do amigo $friendUsername: ${votesError.message}")
                                    return@addSnapshotListener
                                }

                                if (votesSnapshot == null || votesSnapshot.isEmpty) {
                                    println("Nenhum voto encontrado para o amigo $friendUsername")
                                    allFriendsVotes[friendId] = emptyList()
                                    updateFeed()
                                    return@addSnapshotListener
                                }

                                // Processar votos deste amigo
                                val friendVotes = votesSnapshot.documents.mapNotNull { doc ->
                                    try {
                                        val matchId = doc.getString("matchId") ?: return@mapNotNull null
                                        val matchDateTimestamp = doc.getDate("matchDate")
                                        val playerId = doc.getString("playerId") ?: return@mapNotNull null
                                        val playerName = doc.getString("playerName") ?: ""
                                        val playerNickname = doc.getString("playerNickname") ?: ""
                                        val playerImage = doc.getString("playerImage") ?: ""
                                        val playerPositionStr = doc.getString("playerPosition") ?: "TOP"
                                        val teamId = doc.getString("teamId") ?: ""
                                        val teamName = doc.getString("teamName") ?: ""
                                        val teamCode = doc.getString("teamCode") ?: ""
                                        val teamImage = doc.getString("teamImage") ?: ""
                                        val opponentTeamCode = doc.getString("opponentTeamCode") ?: ""
                                        val rating = doc.getDouble("rating")?.toFloat() ?: 0f
                                        val timestampDate = doc.getDate("timestamp")

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

                                        // Criar o voto base
                                        val baseVote = UserVoteHistoryItem(
                                            id = doc.id,
                                            matchId = matchId,
                                            matchDate = matchDate,
                                            playerId = playerId,
                                            playerName = playerName,
                                            playerNickname = playerNickname,
                                            playerImage = playerImage,
                                            playerPosition = playerPosition,
                                            teamId = teamId,
                                            teamName = teamName,
                                            teamCode = teamCode,
                                            teamImage = teamImage,
                                            opponentTeamCode = opponentTeamCode,
                                            rating = rating,
                                            timestamp = timestamp
                                        )

                                        // Criar o item do feed de amigos
                                        FriendVoteHistoryItem(
                                            baseVote = baseVote,
                                            friendId = friendId,
                                            friendUsername = friendUsername
                                        )
                                    } catch (e: Exception) {
                                        println("Erro ao processar voto: ${e.message}")
                                        null
                                    }
                                }

                                // Atualizar o mapa de votos dos amigos
                                allFriendsVotes[friendId] = friendVotes

                                // Combinar todos os votos de todos os amigos e enviar para o fluxo
                                updateFeed()
                            }

                        // Manter referência ao listener para poder removê-lo depois
                        listeners.add(votesListener)
                    }
                }

            // Adicionar o listener principal à lista
            listeners.add(friendsListener)

            // Crucial: fechar todos os listeners quando o flow for cancelado
            awaitClose {
                println("Fechando listeners do feed de amigos")
                listeners.forEach { it.remove() }
            }
        } catch (e: Exception) {
            println("Erro crítico no feed de amigos: ${e.message}")
            e.printStackTrace()
            trySend(emptyList())
            close(e)
        }
    }

    override suspend fun sendFriendRequest(username: String): Result<FriendRequest> {
        return try {
            // Obter o usuário atual
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            // Verificar se não está tentando adicionar a si mesmo
            if (currentUser.username == username) {
                return Result.failure(Exception("Você não pode enviar solicitação para si mesmo"))
            }

            // Buscar o usuário pelo nome de usuário
            val friendSnapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (friendSnapshot.isEmpty) {
                return Result.failure(Exception("Usuário não encontrado"))
            }

            val friendDoc = friendSnapshot.documents.first()
            val friendId = friendDoc.id
            friendDoc.getString("username") ?: ""

            // Verificar se já é amigo
            val existingFriendship = firestore.collection(friendsCollection)
                .document(currentUser.id)
                .collection("friends")
                .document(friendId)
                .get()
                .await()

            if (existingFriendship.exists()) {
                return Result.failure(Exception("Usuário já é seu amigo"))
            }

            // Verificar se já existe uma solicitação pendente
            val existingRequest = firestore.collection(requestsCollection)
                .whereEqualTo("senderId", currentUser.id)
                .whereEqualTo("receiverId", friendId)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()

            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("Solicitação já enviada"))
            }

            // Criar solicitação
            val requestId = "${currentUser.id}_${friendId}"
            val createdAt = Clock.System.now()

            val friendRequest = FriendRequest(
                id = requestId,
                senderId = currentUser.id,
                senderUsername = currentUser.username,
                receiverId = friendId,
                status = FriendRequestStatus.PENDING,
                createdAt = createdAt
            )

            // Salvar solicitação
            firestore.collection(requestsCollection)
                .document(requestId)
                .set(
                    mapOf(
                        "id" to friendRequest.id,
                        "senderId" to friendRequest.senderId,
                        "senderUsername" to friendRequest.senderUsername,
                        "receiverId" to friendRequest.receiverId,
                        "status" to friendRequest.status.name,
                        "createdAt" to Date.from(createdAt.toJavaInstant())
                    )
                )
                .await()

            Result.success(friendRequest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptFriendRequest(requestId: String): Result<Friendship> {
        return try {
            // Obter o usuário atual
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            // Buscar a solicitação
            val requestDoc = firestore.collection(requestsCollection)
                .document(requestId)
                .get()
                .await()

            if (!requestDoc.exists()) {
                return Result.failure(Exception("Solicitação não encontrada"))
            }

            val senderId = requestDoc.getString("senderId") ?: ""
            val senderUsername = requestDoc.getString("senderUsername") ?: ""
            val receiverId = requestDoc.getString("receiverId") ?: ""
            val status = requestDoc.getString("status")

            // Verificar se é o destinatário da solicitação
            if (receiverId != currentUser.id) {
                return Result.failure(Exception("Solicitação não é para este usuário"))
            }

            // Verificar status da solicitação
            if (status != "PENDING") {
                return Result.failure(Exception("Solicitação já processada"))
            }

            // Atualizar status da solicitação
            firestore.collection(requestsCollection)
                .document(requestId)
                .update("status", "ACCEPTED")
                .await()

            // Criar amizade para ambos os usuários
            val createdAt = Clock.System.now()

            // Amizade para o receptor (usuário atual)
            val receiverFriendship = Friendship(
                id = "${currentUser.id}_${senderId}",
                userId = currentUser.id,
                friendId = senderId,
                friendUsername = senderUsername,
                createdAt = createdAt
            )

            // Amizade para o remetente
            val senderFriendship = Friendship(
                id = "${senderId}_${currentUser.id}",
                userId = senderId,
                friendId = currentUser.id,
                friendUsername = currentUser.username,
                createdAt = createdAt
            )

            // Salvar amizade para o receptor
            firestore.collection(friendsCollection)
                .document(currentUser.id)
                .collection("friends")
                .document(senderId)
                .set(
                    mapOf(
                        "id" to receiverFriendship.id,
                        "userId" to receiverFriendship.userId,
                        "friendId" to receiverFriendship.friendId,
                        "friendUsername" to receiverFriendship.friendUsername,
                        "createdAt" to Date.from(createdAt.toJavaInstant())
                    )
                )
                .await()

            // Salvar amizade para o remetente
            firestore.collection(friendsCollection)
                .document(senderId)
                .collection("friends")
                .document(currentUser.id)
                .set(
                    mapOf(
                        "id" to senderFriendship.id,
                        "userId" to senderFriendship.userId,
                        "friendId" to senderFriendship.friendId,
                        "friendUsername" to senderFriendship.friendUsername,
                        "createdAt" to Date.from(createdAt.toJavaInstant())
                    )
                )
                .await()

            Result.success(receiverFriendship)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            // Obter o usuário atual
            val currentUser = userRepository.getCurrentUser().first()
                ?: return Result.failure(Exception("Usuário não autenticado"))

            // Buscar a solicitação
            val requestDoc = firestore.collection(requestsCollection)
                .document(requestId)
                .get()
                .await()

            if (!requestDoc.exists()) {
                return Result.failure(Exception("Solicitação não encontrada"))
            }

            val receiverId = requestDoc.getString("receiverId") ?: ""
            val status = requestDoc.getString("status")

            // Verificar se é o destinatário da solicitação
            if (receiverId != currentUser.id) {
                return Result.failure(Exception("Solicitação não é para este usuário"))
            }

            // Verificar status da solicitação
            if (status != "PENDING") {
                return Result.failure(Exception("Solicitação já processada"))
            }

            // Atualizar status da solicitação
            firestore.collection(requestsCollection)
                .document(requestId)
                .update("status", "REJECTED")
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPendingFriendRequests(): Flow<List<FriendRequest>> = callbackFlow {
        // Obter o usuário atual
        try {
            val currentUser = userRepository.getCurrentUser().first()
            if (currentUser == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            // Configurar o listener para solicitações pendentes
            val listener = firestore.collection(requestsCollection)
                .whereEqualTo("receiverId", currentUser.id)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("Erro ao buscar solicitações: ${error.message}")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val requests = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val senderId = doc.getString("senderId") ?: return@mapNotNull null
                            val senderUsername = doc.getString("senderUsername") ?: ""
                            val receiverId = doc.getString("receiverId") ?: return@mapNotNull null
                            val statusStr = doc.getString("status") ?: "PENDING"
                            val createdAtTimestamp = doc.getDate("createdAt")

                            val status = try {
                                FriendRequestStatus.valueOf(statusStr)
                            } catch (_: Exception) {
                                FriendRequestStatus.PENDING
                            }

                            val createdAt = if (createdAtTimestamp != null) {
                                Instant.fromEpochMilliseconds(createdAtTimestamp.time)
                            } else {
                                Clock.System.now()
                            }

                            FriendRequest(
                                id = id,
                                senderId = senderId,
                                senderUsername = senderUsername,
                                receiverId = receiverId,
                                status = status,
                                createdAt = createdAt
                            )
                        } catch (e: Exception) {
                            println("Erro ao processar solicitação: ${e.message}")
                            null
                        }
                    }

                    trySend(requests)
                }

            awaitClose {
                listener.remove()
            }
        } catch (e: Exception) {
            println("Erro ao buscar solicitações: ${e.message}")
            trySend(emptyList())
            close(e)
        }
    }
}