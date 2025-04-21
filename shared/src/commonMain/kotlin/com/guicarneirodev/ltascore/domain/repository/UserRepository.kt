package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun isUserLoggedIn(): Flow<Boolean>

    fun getCurrentUser(): Flow<User?>

    suspend fun signIn(email: String, password: String): Result<User>

    suspend fun signUp(email: String, password: String, username: String): Result<User>

    suspend fun updateProfile(user: User): Result<User>

    suspend fun resetPassword(email: String): Result<Unit>

    suspend fun signOut()

    suspend fun updateFavoriteTeam(teamId: String): Result<Unit>

    suspend fun refreshCurrentUser()
}