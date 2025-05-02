package com.guicarneirodev.ltascore.android.data.repository

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