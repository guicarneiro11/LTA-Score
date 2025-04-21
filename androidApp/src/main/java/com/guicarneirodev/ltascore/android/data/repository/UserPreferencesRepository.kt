package com.guicarneirodev.ltascore.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private val votedMatchPrefix = "voted_match_"

    suspend fun markMatchVoted(userId: String, matchId: String) {
        val key = stringPreferencesKey("$votedMatchPrefix${userId}_$matchId")
        context.dataStore.edit { preferences ->
            preferences[key] = "true"
        }
    }

    fun hasUserVotedForMatch(userId: String, matchId: String): Flow<Boolean> {
        val key = stringPreferencesKey("$votedMatchPrefix${userId}_$matchId")
        return context.dataStore.data.map { preferences ->
            preferences[key] == "true"
        }
    }
}