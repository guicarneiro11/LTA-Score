package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * Verifica se o usuário está autenticado
     */
    fun isUserLoggedIn(): Flow<Boolean>

    /**
     * Obtém dados do usuário atual
     */
    fun getCurrentUser(): Flow<User?>

    /**
     * Autentica um usuário com email e senha
     */
    suspend fun signIn(email: String, password: String): Result<User>

    /**
     * Cria uma nova conta de usuário
     */
    suspend fun signUp(email: String, password: String, username: String): Result<User>

    /**
     * Atualiza o perfil do usuário
     */
    suspend fun updateProfile(user: User): Result<User>

    /**
     * Envia um email de recuperação de senha
     */
    suspend fun resetPassword(email: String): Result<Unit>

    /**
     * Realiza logout
     */
    suspend fun signOut()
}