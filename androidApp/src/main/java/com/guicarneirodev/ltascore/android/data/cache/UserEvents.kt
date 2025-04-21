package com.guicarneirodev.ltascore.android.data.cache

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object UserEvents {
    private val _userUpdated = MutableSharedFlow<String>()
    val userUpdated: SharedFlow<String> = _userUpdated.asSharedFlow()

    suspend fun notifyUserUpdated(userId: String) {
        _userUpdated.emit(userId)
    }
}