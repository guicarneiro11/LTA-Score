package com.guicarneirodev.ltascore.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.guicarneirodev.ltascore.android.data.cache.FavoriteTeamCache
import com.guicarneirodev.ltascore.android.data.repository.UserPreferencesRepository
import com.guicarneirodev.ltascore.android.ui.admin.AdminMatchPlayersScreen
import com.guicarneirodev.ltascore.android.ui.auth.LoginScreen
import com.guicarneirodev.ltascore.android.ui.auth.RegisterScreen
import com.guicarneirodev.ltascore.android.ui.auth.ResetPasswordScreen
import com.guicarneirodev.ltascore.android.ui.friends.FriendsFeedScreen
import com.guicarneirodev.ltascore.android.ui.history.VoteHistoryScreen
import com.guicarneirodev.ltascore.android.ui.matches.MatchesScreen
import com.guicarneirodev.ltascore.android.ui.notification.NotificationSettingsScreen
import com.guicarneirodev.ltascore.android.ui.profile.EditProfileScreen
import com.guicarneirodev.ltascore.android.ui.profile.ProfileScreen
import com.guicarneirodev.ltascore.android.ui.ranking.RankingScreen
import com.guicarneirodev.ltascore.android.ui.summary.MatchSummaryScreen
import com.guicarneirodev.ltascore.android.ui.voting.VotingScreen
import com.guicarneirodev.ltascore.android.viewmodels.AuthViewModel
import com.guicarneirodev.ltascore.android.viewmodels.FriendsViewModel
import com.guicarneirodev.ltascore.android.viewmodels.MatchSummaryViewModel
import com.guicarneirodev.ltascore.domain.models.MatchState
import com.guicarneirodev.ltascore.domain.repository.AdminRepository
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import com.guicarneirodev.ltascore.domain.usecases.GetMatchByIdUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ResetPassword : Screen("reset_password")
    object Matches : Screen("matches")
    object Profile : Screen("profile?teamId={teamId}") {
        fun createRoute(teamId: String? = null): String {
            return if (teamId != null) "profile?teamId=$teamId" else "profile"
        }
    }
    object Ranking : Screen("ranking")
    object VoteHistory : Screen("vote_history")
    object FriendsFeed : Screen("friends_feed")
    object EditProfile : Screen("edit_profile")

    object Voting : Screen("voting/{matchId}?isAdmin={isAdmin}") {
        fun createRoute(matchId: String, isAdmin: Boolean = false) =
            "voting/$matchId?isAdmin=$isAdmin"
    }

    object MatchSummary : Screen("match_summary/{matchId}") {
        fun createRoute(matchId: String) = "match_summary/$matchId"
    }

    object NotificationSettings : Screen("notification_settings")

    object AdminMatchPlayers : Screen("admin_match_players/{matchId}") {
        fun createRoute(matchId: String) = "admin_match_players/$matchId"
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
        authGraph(
            navController = navController,
            authViewModel = authViewModel
        )

        friendshipGraph(
            navController = navController,
            authViewModel = authViewModel
        )

        composable(Screen.Matches.route) {
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Matches.route) { inclusive = true }
                    }
                }
            }

            MatchesScreen(
                onMatchClick = { matchId ->
                    navigateToMatchDetails(
                        navController = navController,
                        matchId = matchId,
                        userRepository = userRepository,
                        voteRepository = voteRepository,
                        userPreferencesRepository = userPreferencesRepository,
                    )
                },
                onAdminClick = { matchId ->
                    navController.navigate(Screen.AdminMatchPlayers.createRoute(matchId))
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onRankingClick = {
                    navController.navigate(Screen.Ranking.route)
                }
            )
        }

        composable(
            route = Screen.AdminMatchPlayers.route,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""

            AdminMatchPlayersScreen(
                matchId = matchId,
                onBackClick = { navController.popBackStack() },
                onNavigateToMatchSummary = { navController.navigate(Screen.MatchSummary.createRoute(it)) },
                onNavigateToVoting = { navController.navigate(Screen.Voting.createRoute(it)) }
            )
        }

        composable(Screen.Ranking.route) {
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

        composable(
            route = Screen.Profile.route,
            arguments = listOf(
                navArgument("teamId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val friendsViewModel = koinViewModel<FriendsViewModel>()

            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Profile.route) { inclusive = true }
                    }
                }
            }

            val teamId = backStackEntry.arguments?.getString("teamId")

            LaunchedEffect(teamId) {
                if (teamId != null) {
                    FavoriteTeamCache.updateFavoriteTeam(teamId)
                    authViewModel.triggerUserRefresh()
                }
            }

            ProfileScreen(
                friendsViewModel = friendsViewModel,
                authViewModel = authViewModel,
                onNavigateToMatchHistory = {
                    navController.navigate(Screen.VoteHistory.route)
                },
                onNavigateToRanking = {
                    navController.navigate(Screen.Ranking.route)
                },
                onNavigateToFriendsFeed = {
                    navController.navigate(Screen.FriendsFeed.route)
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateToNotificationSettings = {
                    navController.navigate(Screen.NotificationSettings.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Matches.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.navigate(Screen.Matches.route) {
                        popUpTo(Screen.Matches.route) { inclusive = true }
                    }
                },
                uiState = friendsViewModel.uiState.collectAsState().value,
                onViewFriendsFeed = {
                    navController.navigate(Screen.FriendsFeed.route)
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveComplete = { teamId ->
                    navController.navigate(
                        Screen.Profile.createRoute(teamId)
                    ) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = false
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            )
        }

        composable(
            route = Screen.Voting.route,
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("isAdmin") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            val isAdmin = backStackEntry.arguments?.getBoolean("isAdmin") ?: false

            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Matches.route)
                    }
                }
            }

            LaunchedEffect(matchId, isLoggedIn) {
                if (isLoggedIn) {
                    val currentUser = userRepository.getCurrentUser().first()
                    if (currentUser != null) {
                        val hasVotedLocally = userPreferencesRepository.hasUserVotedForMatch(currentUser.id, matchId).first()
                        if (hasVotedLocally) {
                            navController.navigate(Screen.MatchSummary.createRoute(matchId)) {
                                popUpTo(Screen.Voting.route) { inclusive = true }
                            }
                        }
                    }
                }
            }

            VotingScreen(
                matchId = matchId,
                isAdmin = isAdmin,
                onBackClick = { navController.popBackStack() },
                onVoteSubmitted = {
                    navController.navigate(Screen.MatchSummary.createRoute(matchId)) {
                        popUpTo(Screen.Matches.route) {
                            saveState = true
                        }
                    }
                }
            )
        }

        composable(Screen.MatchSummary.route) { backStackEntry ->
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
                    navController.navigate(Screen.Matches.route) {
                        popUpTo(Screen.Matches.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.VoteHistory.route) {
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.VoteHistory.route) { inclusive = true }
                    }
                }
            }

            VoteHistoryScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.NotificationSettings.route) {
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.NotificationSettings.route) { inclusive = true }
                    }
                }
            }

            NotificationSettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

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

private fun navigateToMatchDetails(
    navController: NavHostController,
    matchId: String,
    userRepository: UserRepository,
    voteRepository: VoteRepository,
    userPreferencesRepository: UserPreferencesRepository
) {
    kotlinx.coroutines.MainScope().launch {
        try {
            val currentUser = userRepository.getCurrentUser().first()

            if (currentUser != null) {
                val adminRepository = org.koin.core.context.GlobalContext.get().get<AdminRepository>()
                val isAdmin = try {
                    adminRepository.isUserAdmin(currentUser.id).first()
                } catch (_: Exception) {
                    false
                }

                if (isAdmin) {
                    navController.navigate(Screen.AdminMatchPlayers.createRoute(matchId))
                    return@launch
                }

                val hasVotedLocally = userPreferencesRepository.hasUserVotedForMatch(currentUser.id, matchId).first()
                if (hasVotedLocally) {
                    navController.navigate(Screen.MatchSummary.createRoute(matchId))
                    return@launch
                }

                try {
                    val hasVotedInFirestore = voteRepository.hasUserVotedForMatch(currentUser.id, matchId).first()

                    if (hasVotedInFirestore) {
                        userPreferencesRepository.markMatchVoted(currentUser.id, matchId)
                        navController.navigate(Screen.MatchSummary.createRoute(matchId))
                    } else {
                        navController.navigate("voting/${matchId}?isAdmin=${isAdmin}")
                    }
                } catch (_: Exception) {
                    navController.navigate("voting/${matchId}?isAdmin=${isAdmin}")
                }
            } else {
                navController.navigate(Screen.Login.route)
            }
        } catch (_: Exception) {
            navController.navigate(Screen.Voting.createRoute(matchId))
        }
    }
}

fun NavGraphBuilder.friendshipGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    composable(Screen.FriendsFeed.route) {
        LaunchedEffect(key1 = true) {
            if (!authViewModel.isLoggedIn.first()) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.FriendsFeed.route) { inclusive = true }
                }
            }
        }

        FriendsFeedScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}