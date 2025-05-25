package com.guicarneirodev.ltascore.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.guicarneirodev.ltascore.android.viewmodels.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")
    private val dataStore = context.dataStore

    companion object {
        private const val VOTED_MATCHES_PREFIX = "voted_match_"
        private val MATCH_SORT_ORDER = stringPreferencesKey("match_sort_order")
    }

    suspend fun markMatchVoted(userId: String, matchId: String) {
        val key = booleanPreferencesKey("${VOTED_MATCHES_PREFIX}${userId}_$matchId")
        dataStore.edit { preferences ->
            preferences[key] = true
        }
    }

    fun hasUserVotedForMatch(userId: String, matchId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("${VOTED_MATCHES_PREFIX}${userId}_$matchId")
        return dataStore.data.map { preferences ->
            preferences[key] ?: false
        }
    }

    suspend fun setMatchSortOrder(sortOrder: SortOrder) {
        dataStore.edit { preferences ->
            preferences[MATCH_SORT_ORDER] = sortOrder.name
        }
    }

    fun getMatchSortOrder(): Flow<SortOrder> {
        return dataStore.data.map { preferences ->
            val sortOrderString = preferences[MATCH_SORT_ORDER] ?: SortOrder.OLDEST_FIRST.name
            try {
                SortOrder.valueOf(sortOrderString)
            } catch (e: IllegalArgumentException) {
                SortOrder.OLDEST_FIRST
            }
        }
    }
}