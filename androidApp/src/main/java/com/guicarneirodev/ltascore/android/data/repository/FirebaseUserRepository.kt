package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
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
    private val coroutineScope = CoroutineScope(Dispatchers.IO) // Escopo de coroutine para operações de IO

    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                // Se o usuário está logado, busca os dados completos em uma coroutine
                coroutineScope.launch {
                    try {
                        val user = getUserData(auth.currentUser!!)
                        trySend(user)
                    } catch (e: Exception) {
                        // Em caso de falha ao buscar dados, emite o usuário básico
                        trySend(createDefaultUser(auth.currentUser!!))
                    }
                }
            } else {
                // Se não está logado, emite null
                trySend(null)
            }
        }

        auth.addAuthStateListener(authStateListener)

        // Emite o estado atual imediatamente
        if (auth.currentUser != null) {
            coroutineScope.launch {
                try {
                    val user = getUserData(auth.currentUser!!)
                    trySend(user)
                } catch (e: Exception) {
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

        // Emite o estado atual imediatamente
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

    override suspend fun signUp(email: String, password: String, username: String): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user!!

            // Criar documento do usuário no Firestore
            val user = User(
                id = firebaseUser.uid,
                email = email,
                username = username,
                createdAt = Clock.System.now()
            )

            usersCollection.document(user.id).set(user).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(user: User): Result<User> {
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
        // Tenta buscar dados do usuário no Firestore
        val documentSnapshot = usersCollection.document(firebaseUser.uid).get().await()

        // Se o documento existe, converte para User
        return if (documentSnapshot.exists()) {
            val userData = documentSnapshot.data
            if (userData != null) {
                User(
                    id = firebaseUser.uid,
                    email = userData["email"] as? String ?: "",
                    username = userData["username"] as? String ?: "",
                    profilePictureUrl = userData["profilePictureUrl"] as? String,
                    createdAt = Clock.System.now() // Precisaríamos converter Timestamp para Instant
                )
            } else {
                createDefaultUser(firebaseUser)
            }
        } else {
            // Se não existe, cria um User básico
            val defaultUser = createDefaultUser(firebaseUser)

            // Salva o usuário padrão no Firestore para futuras consultas
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
            createdAt = Clock.System.now()
        )
    }
}