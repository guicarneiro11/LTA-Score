package com.guicarneirodev.ltascore.android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.guicarneirodev.ltascore.android.MainActivity
import com.guicarneirodev.ltascore.android.R
import com.guicarneirodev.ltascore.android.util.StringResources

class LTAScoreMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "match_updates"
        private const val NOTIFICATION_ID = 1
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "LTA Score"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val matchId = message.data["matchId"]

        createNotificationChannel()
        showNotification(title, body, matchId)
    }

    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("user_tokens")
            .document(userId)
            .set(mapOf("token" to token, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener {
                println("FCM Token saved to Firestore")
            }
            .addOnFailureListener { e ->
                println("Failed to save FCM Token: ${e.message}")
            }
    }

    private fun createNotificationChannel() {
        val name = StringResources.getString(R.string.match_updates_channel_name)
        val descriptionText = StringResources.getString(R.string.match_updates_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableLights(true)
            enableVibration(true)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showNotification(title: String, body: String, matchId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            matchId?.let { putExtra("MATCH_ID", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}