package com.guicarneirodev.ltascore.android.data.cache

object FavoriteTeamCache {
    private var lastSelectedTeamId: String? = null
    private var lastUpdateTimestamp: Long = 0

    private const val CACHE_VALIDITY_MS = 5 * 60 * 1000

    fun getFavoriteTeam(): String? {
        val isCacheValid = System.currentTimeMillis() - lastUpdateTimestamp < CACHE_VALIDITY_MS
        return if (isCacheValid) lastSelectedTeamId else null
    }

    fun updateFavoriteTeam(teamId: String?) {
        lastSelectedTeamId = teamId
        lastUpdateTimestamp = System.currentTimeMillis()
        println("Cache de time favorito atualizado: $teamId")
    }
}