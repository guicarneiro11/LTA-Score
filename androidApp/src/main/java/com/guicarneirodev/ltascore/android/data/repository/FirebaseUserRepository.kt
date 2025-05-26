package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.ltascore.android.data.cache.FavoriteTeamCache
import com.guicarneirodev.ltascore.android.data.cache.UserEvents
import com.guicarneirodev.ltascore.domain.models.User
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock

class FirebaseUserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private val usersCollection = firestore.collection("users")
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        var lastUser: User? = null

        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            coroutineScope.launch {
                if (auth.currentUser != null) {
                    try {
                        val user = getUserData(auth.currentUser!!)

                        val shouldUpdate = lastUser == null ||
                                lastUser?.id != user.id ||
                                lastUser?.favoriteTeamId != user.favoriteTeamId

                        if (shouldUpdate) {
                            lastUser = user
                            println("Emitindo usuário com novo estado: id=${user.id}, time=${user.favoriteTeamId}")
                            trySend(user)
                        }
                    } catch (e: Exception) {
                        println("Erro ao buscar dados do usuário: ${e.message}")
                        trySend(createDefaultUser(auth.currentUser!!))
                    }
                } else {
                    lastUser = null
                    trySend(null)
                }
            }
        }

        auth.addAuthStateListener(authStateListener)

        if (auth.currentUser != null) {
            coroutineScope.launch {
                try {
                    val user = getUserData(auth.currentUser!!)
                    trySend(user)
                    println("Emissão inicial de usuário: ${user.id}, time: ${user.favoriteTeamId}")
                } catch (e: Exception) {
                    println("Erro na emissão inicial: ${e.message}")
                    trySend(createDefaultUser(auth.currentUser!!))
                }
            }
        } else {
            trySend(null)
        }

        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    override fun isUserLoggedIn(): Flow<Boolean> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }

        auth.addAuthStateListener(authStateListener)

        trySend(auth.currentUser != null)

        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = getUserData(authResult.user!!)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun isUsernameAlreadyTaken(username: String): Boolean {
        try {
            println("Verificando se o username '$username' já existe...")

            val querySnapshot = usersCollection
                .whereEqualTo("username", username)
                .limit(1)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()

            val usernameExists = !querySnapshot.isEmpty
            println("Username '$username' ${if (usernameExists) "já existe" else "disponível"}")
            return usernameExists
        } catch (e: Exception) {
            println("Erro ao verificar username: ${e.message}")

            println("Detalhes técnicos do erro:")
            e.printStackTrace()

            return true
        }
    }

    override suspend fun signUp(email: String, password: String, username: String): Result<User> {
        try {
            val lowercaseUsername = username.lowercase()

            val usernameDoc = firestore
                .collection("usernames")
                .document(lowercaseUsername)
                .get()
                .await()

            if (usernameDoc.exists()) {
                return Result.failure(Exception("Nome de usuário já está em uso"))
            }

            if (username.length < 3 || username.length > 20) {
                return Result.failure(Exception("Nome de usuário deve ter entre 3 e 20 caracteres"))
            }

            val usernameRegex = Regex("^[a-zA-Z0-9_]+$")
            if (!usernameRegex.matches(username)) {
                return Result.failure(Exception("Nome de usuário inválido. Use apenas letras, números e _"))
            }

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user!!

            val user = User(
                id = firebaseUser.uid,
                email = email,
                username = username,
                createdAt = Clock.System.now()
            )

            usersCollection.document(user.id).set(user).await()

            firestore.collection("usernames")
                .document(lowercaseUsername)
                .set(mapOf(
                    "username" to lowercaseUsername,
                    "userId" to user.id
                ))
                .await()

            return Result.success(user)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun updateProfile(user: User): Result<User> {
        if (user.username != getUserData(auth.currentUser!!).username &&
            isUsernameAlreadyTaken(user.username)) {
            return Result.failure(Exception("Nome de usuário já está em uso"))
        }

        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getUserData(firebaseUser: FirebaseUser): User {
        val documentSnapshot = usersCollection.document(firebaseUser.uid).get().await()

        return if (documentSnapshot.exists()) {
            val userData = documentSnapshot.data
            if (userData != null) {
                User(
                    id = firebaseUser.uid,
                    email = userData["email"] as? String ?: "",
                    username = userData["username"] as? String ?: "",
                    profilePictureUrl = userData["profilePictureUrl"] as? String,
                    favoriteTeamId = userData["favoriteTeamId"] as? String,
                    createdAt = Clock.System.now()
                )
            } else {
                createDefaultUser(firebaseUser)
            }
        } else {
            val defaultUser = createDefaultUser(firebaseUser)

            usersCollection.document(firebaseUser.uid).set(defaultUser).await()

            defaultUser
        }
    }

    private fun createDefaultUser(firebaseUser: FirebaseUser): User {
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            username = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@') ?: "Usuário",
            profilePictureUrl = firebaseUser.photoUrl?.toString(),
            favoriteTeamId = null,
            createdAt = Clock.System.now()
        )
    }

    override suspend fun refreshCurrentUser() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            try {
                val updatedUserDoc = usersCollection.document(firebaseUser.uid)
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .await()

                if (updatedUserDoc.exists()) {
                    val userData = updatedUserDoc.data
                    if (userData != null) {
                        val favoriteTeamId = userData["favoriteTeamId"] as? String

                        FavoriteTeamCache.updateFavoriteTeam(favoriteTeamId)

                        println("Dados do usuário recarregados com sucesso: $favoriteTeamId")
                    }
                }
            } catch (e: Exception) {
                println("Erro ao recarregar dados do usuário: ${e.message}")
            }
        }
    }

    override suspend fun updateFavoriteTeam(teamId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuário não autenticado"))

            usersCollection.document(currentUser.uid)
                .update("favoriteTeamId", teamId)
                .await()

            FavoriteTeamCache.updateFavoriteTeam(teamId)

            val updatedUserDoc = usersCollection.document(currentUser.uid)
                .get()
                .await()

            if (updatedUserDoc.exists()) {
                coroutineScope.launch(Dispatchers.Main) {
                    try {
                        auth.currentUser?.let { firebaseUser ->
                            try {
                                auth.updateCurrentUser(firebaseUser).await()
                            } catch (e: Exception) {
                                println("Erro ao forçar atualização: ${e.message}")
                            }
                        }

                        UserEvents.notifyUserUpdated(currentUser.uid)
                        println("Time favorito atualizado para: $teamId")
                    } catch (e: Exception) {
                        println("Erro ao notificar atualização: ${e.message}")
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}