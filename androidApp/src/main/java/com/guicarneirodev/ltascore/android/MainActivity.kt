package com.guicarneirodev.ltascore.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.guicarneirodev.ltascore.android.data.repository.UserPreferencesRepository
import com.guicarneirodev.ltascore.api.LoLEsportsApi
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
    }
}