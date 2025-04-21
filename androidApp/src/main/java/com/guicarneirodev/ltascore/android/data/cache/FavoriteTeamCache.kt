package com.guicarneirodev.ltascore.android.data.cache

object FavoriteTeamCache {
    private var lastSelectedTeamId: String? = null

    private var lastUpdateTimestamp: Long = 0

    fun getFavoriteTeam(): String? {
        val isCacheValid = System.currentTimeMillis() - lastUpdateTimestamp < 30_000
        return if (isCacheValid) lastSelectedTeamId else null
    }
}