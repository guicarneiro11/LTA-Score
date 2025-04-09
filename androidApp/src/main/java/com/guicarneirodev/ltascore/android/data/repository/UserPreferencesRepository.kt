package com.guicarneirodev.ltascore.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property para acessar o DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Repositório para gerenciar preferências do usuário usando DataStore
 */
class UserPreferencesRepository(private val context: Context) {

    // Prefixo para as chaves de votação
    private val VOTED_MATCH_PREFIX = "voted_match_"

    /**
     * Marca que o usuário votou em uma partida específica
     */
    suspend fun markMatchVoted(userId: String, matchId: String) {
        // A chave tem formato: voted_match_{userId}_{matchId}
        val key = stringPreferencesKey("$VOTED_MATCH_PREFIX${userId}_$matchId")
        context.dataStore.edit { preferences ->
            preferences[key] = "true"
        }
    }

    /**
     * Verifica se o usuário já votou em uma partida específica
     */
    fun hasUserVotedForMatch(userId: String, matchId: String): Flow<Boolean> {
        val key = stringPreferencesKey("$VOTED_MATCH_PREFIX${userId}_$matchId")
        return context.dataStore.data.map { preferences ->
            preferences[key] == "true"
        }
    }

    /**
     * Lista todas as partidas em que o usuário já votou
     */
    fun getUserVotedMatches(userId: String): Flow<List<String>> {
        return context.dataStore.data.map { preferences ->
            preferences.asMap().keys
                .filter { it.name.startsWith("$VOTED_MATCH_PREFIX$userId") }
                .map { it.name.removePrefix("$VOTED_MATCH_PREFIX$userId"+"_") }
        }
    }

    /**
     * Limpa todos os registros de votação do usuário
     * Útil para debug ou reset de conta
     */
    suspend fun clearAllVoteRecords(userId: String) {
        context.dataStore.edit { preferences ->
            // Remove todas as chaves que começam com o prefixo do usuário
            val keysToRemove = preferences.asMap().keys.filter {
                it.name.startsWith("$VOTED_MATCH_PREFIX$userId")
            }
            keysToRemove.forEach { preferences.remove(it) }
        }
    }
}