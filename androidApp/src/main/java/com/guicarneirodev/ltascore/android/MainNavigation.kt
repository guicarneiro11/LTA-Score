package com.guicarneirodev.ltascore.android

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.guicarneirodev.ltascore.android.ui.voting.VotingScreen
import com.guicarneirodev.ltascore.android.ui.matches.MatchesScreen
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "matches",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("matches") {
                MatchesScreen(
                    viewModel = koinViewModel(),
                    onMatchClick = { matchId ->
                        navController.navigate("voting/$matchId")
                    }
                )
            }
            composable("voting/{matchId}") { backStackEntry ->
                val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
                VotingScreen(
                    viewModel = koinViewModel(),
                    matchId = matchId,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}