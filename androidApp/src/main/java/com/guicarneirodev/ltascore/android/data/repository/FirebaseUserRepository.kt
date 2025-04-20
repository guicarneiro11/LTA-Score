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
                    } catch (_: Exception) {
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
                } catch (_: Exception) {
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

    /**
     * Verifica se um nome de usuário já está em uso
     * @return true se o nome de usuário já existe, false caso contrário
     */
    private suspend fun isUsernameAlreadyTaken(username: String): Boolean {
        try {
            println("Verificando se o username '$username' já existe...")

            // Abordagem alternativa usando get() diretamente
            // Verificamos os primeiros 10 usuários para evitar problemas de ordenação
            val querySnapshot = usersCollection
                .whereEqualTo("username", username)
                .limit(1)  // Limitamos a apenas 1 resultado
                .get(com.google.firebase.firestore.Source.SERVER)  // Forçamos busca no servidor
                .await()

            val usernameExists = !querySnapshot.isEmpty
            println("Username '$username' ${if (usernameExists) "já existe" else "disponível"}")
            return usernameExists
        } catch (e: Exception) {
            // Log detalhado do erro para depuração
            println("Erro ao verificar username: ${e.message}")

            // Mostrar mais detalhes técnicos para depuração
            println("Detalhes técnicos do erro:")
            e.printStackTrace()

            // Em caso de erro na consulta, assumimos que o username está em uso (por segurança)
            return true
        }
    }

    override suspend fun signUp(email: String, password: String, username: String): Result<User> {
        try {
            // Primeiro, verificar disponibilidade do username
            val lowercaseUsername = username.lowercase()

            // Verificação direta sem autenticação
            val usernameDoc = firestore
                .collection("usernames")
                .document(lowercaseUsername)
                .get()
                .await()

            if (usernameDoc.exists()) {
                return Result.failure(Exception("Nome de usuário já está em uso"))
            }

            // Validações adicionais de username
            if (username.length < 3 || username.length > 20) {
                return Result.failure(Exception("Nome de usuário deve ter entre 3 e 20 caracteres"))
            }

            val usernameRegex = Regex("^[a-zA-Z0-9_]+$")
            if (!usernameRegex.matches(username)) {
                return Result.failure(Exception("Nome de usuário inválido. Use apenas letras, números e _"))
            }

            // Continua processo de criação de conta
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user!!

            // Criar documento do usuário
            val user = User(
                id = firebaseUser.uid,
                email = email,
                username = username,
                createdAt = Clock.System.now()
            )

            // Salvar usuário
            usersCollection.document(user.id).set(user).await()

            // Reservar username
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

    suspend fun isUsernameTaken(username: String): Boolean {
        return try {
            val lowercaseUsername = username.lowercase()
            val snapshot = firestore
                .collection("usernames")
                .document(lowercaseUsername)
                .get()
                .await()

            snapshot.exists()
        } catch (e: Exception) {
            // Em caso de erro, assume que o username está indisponível
            true
        }
    }

    override suspend fun updateProfile(user: User): Result<User> {
        // Primeiro verificar se o novo nome de usuário não conflita com outro usuário
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
                    favoriteTeamId = userData["favoriteTeamId"] as? String, // Adicionado essa linha
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
            favoriteTeamId = null,
            createdAt = Clock.System.now()
        )
    }

    override suspend fun updateFavoriteTeam(teamId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Usuário não autenticado"))

            usersCollection.document(currentUser.uid)
                .update("favoriteTeamId", teamId)
                .await()

            val updatedUser = getUserData(currentUser)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}