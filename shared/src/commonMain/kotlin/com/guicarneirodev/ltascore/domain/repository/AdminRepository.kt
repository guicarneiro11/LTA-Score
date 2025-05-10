package com.guicarneirodev.ltascore.domain.repository

import kotlinx.coroutines.flow.Flow

interface AdminRepository {
    fun isUserAdmin(userId: String): Flow<Boolean>
}