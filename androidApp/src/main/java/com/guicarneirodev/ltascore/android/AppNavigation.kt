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
import com.guicarneirodev.ltascore.android.data.repository.UserPreferencesRepository
import com.guicarneirodev.ltascore.android.ui.auth.LoginScreen
import com.guicarneirodev.ltascore.android.ui.auth.RegisterScreen
import com.guicarneirodev.ltascore.android.ui.auth.ResetPasswordScreen
import com.guicarneirodev.ltascore.android.ui.matches.MatchesScreen
import com.guicarneirodev.ltascore.android.ui.profile.ProfileScreen
import com.guicarneirodev.ltascore.android.ui.ranking.RankingScreen
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
    object Profile : Screen("profile")
    object Ranking : Screen("ranking") // Nova tela de ranking

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
    voteRepository: VoteRepository,
    userPreferencesRepository: UserPreferencesRepository
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
                        voteRepository = voteRepository,
                        userPreferencesRepository = userPreferencesRepository
                    )
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onRankingClick = {
                    navController.navigate(Screen.Ranking.route)
                }
            )
        }

        // Nova tela de Ranking
        composable(Screen.Ranking.route) {
            // Verifica se o usuário está autenticado
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Ranking.route) { inclusive = true }
                    }
                }
            }

            RankingScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Tela de perfil do usuário
        composable(Screen.Profile.route) {
            // Verifica se o usuário está autenticado
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Profile.route) { inclusive = true }
                    }
                }
            }

            ProfileScreen(
                onNavigateToEditProfile = {
                    // Temporariamente, apenas mostra o perfil
                    // No futuro, implementar tela de edição
                },
                onNavigateToMatchHistory = {
                    // Navega de volta para partidas por enquanto
                    navController.navigate(Screen.Matches.route)
                },
                onNavigateToRanking = {
                    // Navega para a nova tela de ranking
                    navController.navigate(Screen.Ranking.route)
                },
                onNavigateToSettings = {
                    // Temporariamente, apenas mostra o perfil
                    // No futuro, implementar tela de configurações
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Matches.route) { inclusive = true }
                    }
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

            // Verifica se o usuário já votou nesta partida
            LaunchedEffect(matchId, isLoggedIn) {
                if (isLoggedIn) {
                    val currentUser = userRepository.getCurrentUser().first()
                    if (currentUser != null) {
                        // Verifica no DataStore local
                        val hasVotedLocally = userPreferencesRepository.hasUserVotedForMatch(currentUser.id, matchId).first()
                        if (hasVotedLocally) {
                            // Se já votou, redireciona para a tela de resumo
                            navController.navigate(Screen.MatchSummary.createRoute(matchId)) {
                                popUpTo(Screen.Voting.route) { inclusive = true }
                            }
                        }
                    }
                }
            }

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
                    // Navega para a tela principal de partidas
                    navController.navigate(Screen.Matches.route) {
                        // Configuração para limpar a pilha e evitar múltiplos retornos
                        popUpTo(Screen.Matches.route) { inclusive = true }
                    }
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
    voteRepository: VoteRepository,
    userPreferencesRepository: UserPreferencesRepository
) {
    // Lança uma coroutine para verificar o estado de voto
    kotlinx.coroutines.MainScope().launch {
        try {
            // Obtém o usuário atual
            val currentUser = userRepository.getCurrentUser().first()

            if (currentUser != null) {
                // MODIFICADO: Primeiro verifica no DataStore local (resposta mais rápida)
                val hasVotedLocally = userPreferencesRepository.hasUserVotedForMatch(currentUser.id, matchId).first()

                if (hasVotedLocally) {
                    // Se já temos registro local, navegamos direto para o resumo
                    navController.navigate(Screen.MatchSummary.createRoute(matchId))
                    return@launch
                }

                // Se não temos registro local, verificamos no Firestore
                try {
                    val hasVotedInFirestore = voteRepository.hasUserVotedForMatch(currentUser.id, matchId).first()

                    // Se encontrou voto no Firestore, salva localmente para futuras verificações
                    if (hasVotedInFirestore) {
                        userPreferencesRepository.markMatchVoted(currentUser.id, matchId)
                    }

                    // Navega para a tela apropriada
                    if (hasVotedInFirestore) {
                        navController.navigate(Screen.MatchSummary.createRoute(matchId))
                    } else {
                        navController.navigate(Screen.Voting.createRoute(matchId))
                    }
                } catch (_: Exception) {
                    // Em caso de erro na verificação Firestore, confiamos apenas no registro local
                    // Como não temos registro local, vamos para a tela de votação
                    navController.navigate(Screen.Voting.createRoute(matchId))
                }
            } else {
                // Se o usuário não estiver logado, vai para a tela de login
                navController.navigate(Screen.Login.route)
            }
        } catch (_: Exception) {
            // Em caso de erro geral, vai para a tela de votação (experiência padrão)
            navController.navigate(Screen.Voting.createRoute(matchId))
        }
    }
}