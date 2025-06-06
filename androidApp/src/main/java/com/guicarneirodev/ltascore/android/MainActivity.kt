package com.guicarneirodev.ltascore.android

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        checkAndRegisterFcmToken()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = android.Manifest.permission.POST_NOTIFICATIONS

            when {
                ContextCompat.checkSelfPermission(
                    this,
                    notificationPermission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Notification permission already granted")
                }
                else -> {
                    requestPermissionLauncher.launch(notificationPermission)
                }
            }
        }
    }

    private fun checkAndRegisterFcmToken() {
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
}