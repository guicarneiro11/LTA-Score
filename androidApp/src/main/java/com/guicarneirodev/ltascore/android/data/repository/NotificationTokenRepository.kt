package com.guicarneirodev.ltascore.android.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class NotificationTokenRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()
) {

    suspend fun registerUserForNotifications(userId: String, matchNotifications: Boolean = true, liveMatchNotifications: Boolean = true, resultNotifications: Boolean = true) {
        try {
            val token = messaging.token.await()

            val tokenData = hashMapOf(
                "token" to token,
                "matchNotifications" to matchNotifications,
                "liveMatchNotifications" to liveMatchNotifications,
                "resultNotifications" to resultNotifications,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("user_tokens")
                .document(userId)
                .set(tokenData)
                .await()

            println("User registered for notifications successfully")
        } catch (e: Exception) {
            println("Failed to register for notifications: ${e.message}")
        }
    }

    suspend fun verifyTokensForNotifications(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        try {
            val tokensSnapshot = firestore.collection("user_tokens")
                .get()
                .await()

            val totalTokens = tokensSnapshot.size()
            val validTokens = tokensSnapshot.documents.count { it.contains("token") }
            val matchNotificationsEnabled = tokensSnapshot.documents.count {
                it.getBoolean("matchNotifications") == true
            }
            val liveMatchNotificationsEnabled = tokensSnapshot.documents.count {
                it.getBoolean("liveMatchNotifications") == true
            }
            val resultNotificationsEnabled = tokensSnapshot.documents.count {
                it.getBoolean("resultNotifications") == true
            }

            result["totalTokens"] = totalTokens
            result["validTokens"] = validTokens
            result["matchNotificationsEnabled"] = matchNotificationsEnabled
            result["liveMatchNotificationsEnabled"] = liveMatchNotificationsEnabled
            result["resultNotificationsEnabled"] = resultNotificationsEnabled

            return result
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateNotificationPreferences(
        userId: String,
        matchNotifications: Boolean? = null,
        liveMatchNotifications: Boolean? = null,
        resultNotifications: Boolean? = null
    ) {
        try {
            val updates = mutableMapOf<String, Any>()

            matchNotifications?.let { updates["matchNotifications"] = it }
            liveMatchNotifications?.let { updates["liveMatchNotifications"] = it }
            resultNotifications?.let { updates["resultNotifications"] = it }

            if (updates.isNotEmpty()) {
                updates["updatedAt"] = com.google.firebase.Timestamp.now()

                firestore.collection("user_tokens")
                    .document(userId)
                    .update(updates)
                    .await()

                println("Notification preferences updated")
            }
        } catch (e: Exception) {
            println("Failed to update notification preferences: ${e.message}")
        }
    }

    suspend fun sendTestNotification(userId: String) {
        try {
            val userDoc = firestore.collection("user_tokens")
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) {
                Log.e("TestNotification", "Token não encontrado para usuário $userId")
                return
            }

            val token = userDoc.getString("token")
            if (token.isNullOrEmpty()) {
                Log.e("TestNotification", "Token inválido para usuário $userId")
                return
            }

            Log.d("TestNotification", "Token encontrado para usuário $userId: $token")
            Log.d("TestNotification", "Para testar, envie uma notificação de teste pelo console do Firebase")
        } catch (e: Exception) {
            Log.e("TestNotification", "Erro ao enviar notificação de teste: ${e.message}")
        }
    }

    suspend fun unregisterFromNotifications(userId: String) {
        try {
            firestore.collection("user_tokens")
                .document(userId)
                .delete()
                .await()

            println("User unregistered from notifications")
        } catch (e: Exception) {
            println("Failed to unregister from notifications: ${e.message}")
        }
    }
}