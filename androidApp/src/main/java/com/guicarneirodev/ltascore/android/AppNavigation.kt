package com.guicarneirodev.ltascore.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.guicarneirodev.ltascore.android.ui.auth.LoginScreen
import com.guicarneirodev.ltascore.android.ui.auth.RegisterScreen
import com.guicarneirodev.ltascore.android.ui.auth.ResetPasswordScreen
import com.guicarneirodev.ltascore.android.ui.matches.MatchesScreen
import com.guicarneirodev.ltascore.android.ui.summary.MatchSummaryScreen
import com.guicarneirodev.ltascore.android.ui.voting.VotingScreen
import com.guicarneirodev.ltascore.android.viewmodels.AuthViewModel
import com.guicarneirodev.ltascore.android.viewmodels.MatchSummaryViewModel
import com.guicarneirodev.ltascore.android.viewmodels.VotingViewModel
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ResetPassword : Screen("reset_password")
    object Matches : Screen("matches")

    // Telas com argumentos
    object Voting : Screen("voting/{matchId}") {
        fun createRoute(matchId: String) = "voting/$matchId"
    }

    object MatchSummary : Screen("match_summary/{matchId}") {
        fun createRoute(matchId: String) = "match_summary/$matchId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Matches.route,
    authViewModel: AuthViewModel = koinViewModel(),
    userRepository: UserRepository,
    voteRepository: VoteRepository
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Telas de autenticação
        authGraph(
            navController = navController,
            authViewModel = authViewModel
        )

        // Tela principal de partidas
        composable(Screen.Matches.route) {
            // Verifica se o usuário está autenticado
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Matches.route) { inclusive = true }
                    }
                }
            }

            MatchesScreen(
                onMatchClick = { matchId ->
                    // Decide para qual tela navegar com base no histórico de votos
                    navigateToMatchDetails(
                        navController = navController,
                        matchId = matchId,
                        userRepository = userRepository,
                        voteRepository = voteRepository
                    )
                }
            )
        }

        // Tela de votação
        composable(Screen.Voting.route) { backStackEntry ->
            // Verifica se o usuário está autenticado
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Matches.route)
                    }
                }
            }

            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            val votingViewModel = koinViewModel<VotingViewModel>()

            VotingScreen(
                viewModel = votingViewModel,
                matchId = matchId,
                onBackClick = {
                    navController.popBackStack()
                },
                onVoteSubmitted = {
                    // Navega para a tela de resumo após o voto
                    navController.navigate(Screen.MatchSummary.createRoute(matchId)) {
                        // Remove a tela de votação da pilha para o usuário não conseguir voltar
                        popUpTo(Screen.Matches.route) {
                            saveState = true
                        }
                    }
                }
            )
        }

        // Tela de resumo da partida
        composable(Screen.MatchSummary.route) { backStackEntry ->
            // Verifica se o usuário está autenticado
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Matches.route)
                    }
                }
            }

            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            val summaryViewModel = koinViewModel<MatchSummaryViewModel>()

            MatchSummaryScreen(
                viewModel = summaryViewModel,
                matchId = matchId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Grafo de navegação para telas de autenticação
 */
private fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    composable(Screen.Login.route) {
        LoginScreen(
            viewModel = authViewModel,
            onNavigateToRegistration = {
                navController.navigate(Screen.Register.route)
            },
            onNavigateToResetPassword = {
                navController.navigate(Screen.ResetPassword.route)
            },
            onLoginSuccess = {
                navController.navigate(Screen.Matches.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        )
    }

    composable(Screen.Register.route) {
        RegisterScreen(
            viewModel = authViewModel,
            onNavigateToLogin = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Register.route) { inclusive = true }
                }
            },
            onRegisterSuccess = {
                navController.navigate(Screen.Matches.route) {
                    popUpTo(Screen.Register.route) { inclusive = true }
                }
            }
        )
    }

    composable(Screen.ResetPassword.route) {
        ResetPasswordScreen(
            viewModel = authViewModel,
            onNavigateBack = {
                navController.popBackStack()
            },
            onResetSuccess = {
                navController.popBackStack()
            }
        )
    }
}

/**
 * Função para navegar para a tela apropriada de detalhes da partida
 * Verifica se o usuário já votou e redireciona para a tela correta
 */
private fun navigateToMatchDetails(
    navController: NavHostController,
    matchId: String,
    userRepository: UserRepository,
    voteRepository: VoteRepository
) {
    // Lança uma coroutine para verificar o estado de voto
    kotlinx.coroutines.MainScope().launch {
        try {
            // Obtém o usuário atual
            val currentUser = userRepository.getCurrentUser().first()

            if (currentUser != null) {
                // Verifica se o usuário já votou nesta partida
                val hasVoted = voteRepository.hasUserVotedForMatch(currentUser.id, matchId).first()

                // Navega para a tela apropriada
                if (hasVoted) {
                    navController.navigate(Screen.MatchSummary.createRoute(matchId))
                } else {
                    navController.navigate(Screen.Voting.createRoute(matchId))
                }
            } else {
                // Se o usuário não estiver logado, vai para a tela de login
                navController.navigate(Screen.Login.route)
            }
        } catch (e: Exception) {
            // Em caso de erro, vai para a tela de votação (experiência padrão)
            navController.navigate(Screen.Voting.createRoute(matchId))
        }
    }
}