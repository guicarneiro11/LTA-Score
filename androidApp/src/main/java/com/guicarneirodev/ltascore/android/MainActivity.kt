package com.guicarneirodev.ltascore.android

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.guicarneirodev.ltascore.android.data.repository.NotificationTokenRepository
import com.guicarneirodev.ltascore.android.data.repository.UserPreferencesRepository
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val userRepository: UserRepository by inject()
    private val voteRepository: VoteRepository by inject()
    private val userPreferencesRepository: UserPreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission
        requestNotificationPermission()

        setContent {
            LTAScoreTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        userRepository = userRepository,
                        voteRepository = voteRepository,
                        userPreferencesRepository = userPreferencesRepository
                    )
                }
            }
        }

        // You could also check and handle FCM token registration here
        checkAndRegisterFcmToken()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = android.Manifest.permission.POST_NOTIFICATIONS

            if (ContextCompat.checkSelfPermission(
                    this,
                    notificationPermission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(notificationPermission),
                    100 // Request code
                )
            }
        }
    }

    private fun checkAndRegisterFcmToken() {
        // Only proceed if user is logged in
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            lifecycleScope.launch {
                try {
                    val tokenRepository = NotificationTokenRepository()
                    tokenRepository.registerUserForNotifications(userId)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to register FCM token: ${e.message}")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Notification permission granted")
            } else {
                Log.d("MainActivity", "Notification permission denied")
            }
        }
    }
}