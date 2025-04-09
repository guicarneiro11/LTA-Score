package com.guicarneirodev.ltascore.domain.repository

import com.guicarneirodev.ltascore.domain.models.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    fun isUserLoggedIn(): Flow<Boolean>

    fun getCurrentUser(): Flow<User?>

    suspend fun updateUserProfile(user: User)

    suspend fun logout()
}