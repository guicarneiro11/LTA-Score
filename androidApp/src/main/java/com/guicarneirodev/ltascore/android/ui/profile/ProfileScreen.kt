package com.guicarneirodev.ltascore.android.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import com.guicarneirodev.ltascore.android.R
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.data.cache.FavoriteTeamCache
import com.guicarneirodev.ltascore.android.data.cache.UserEvents
import com.guicarneirodev.ltascore.android.ui.friends.social.FriendRequestsSection
import com.guicarneirodev.ltascore.android.ui.matches.LogoImage
import com.guicarneirodev.ltascore.android.viewmodels.AuthViewModel
import com.guicarneirodev.ltascore.android.viewmodels.EditProfileViewModel
import com.guicarneirodev.ltascore.android.viewmodels.FriendsManagementUiState
import com.guicarneirodev.ltascore.android.viewmodels.FriendsViewModel
import com.guicarneirodev.ltascore.android.viewmodels.TeamFilterItem
import com.guicarneirodev.ltascore.domain.models.Friendship
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    friendsViewModel: FriendsViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
    editProfileViewModel: EditProfileViewModel = koinViewModel(),
    onNavigateToFriendsFeed: () -> Unit,
    onNavigateToMatchHistory: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onLogout: () -> Unit,
    onBackClick: () -> Unit,
    forceUpdate: Long = System.currentTimeMillis(),
    uiState: FriendsManagementUiState,
    onViewFriendsFeed: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val friendsUiState by friendsViewModel.uiState.collectAsState()
    val requestsUiState by friendsViewModel.requestsUiState.collectAsState()
    val editProfileUiState by editProfileViewModel.uiState.collectAsState()

    var showFriendsSection by remember { mutableStateOf(true) }

    var updateTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentUser, updateTrigger) {
        println("ProfileScreen detectou alteração: favoriteTeamId=${currentUser?.favoriteTeamId}, cacheValue=${FavoriteTeamCache.getFavoriteTeam()}, trigger=$updateTrigger")

        val cacheTeamId = FavoriteTeamCache.getFavoriteTeam()
        if (cacheTeamId != null && cacheTeamId != currentUser?.favoriteTeamId) {
            println("Usando valor do cache: $cacheTeamId (diferente de ${currentUser?.favoriteTeamId})")
            editProfileViewModel.selectTeam(cacheTeamId)
        }

        editProfileViewModel.loadTeams()
    }

    LaunchedEffect(Unit) {
        editProfileViewModel.loadAvailableTeams()

        authViewModel.refreshCurrentUser()
    }

    LaunchedEffect(Unit) {
        UserEvents.userUpdated.collect { userId ->
            updateTrigger++
            println("ProfileScreen recebeu evento de atualização, trigger: $updateTrigger")
        }
    }

    LaunchedEffect(key1 = Unit) {
        authViewModel.refreshCurrentUser()
    }

    val teamColors = mapOf(
        "loud" to Color(0xFF33CC33),
        "pain-gaming" to Color(0xFFFFD700),
        "isurus-estral" to Color(0xFF0066CC),
        "leviatan" to Color(0xFFCCCCCC),
        "furia" to Color(0xFF000000),
        "keyd" to Color(0xFFFFFFFF),
        "red" to Color(0xFFFF0000),
        "fxw7" to Color(0xFF9966CC)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_profile)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LTAThemeColors.CardBackground,
                    titleContentColor = LTAThemeColors.TextPrimary,
                    navigationIconContentColor = LTAThemeColors.TextPrimary
                ),
                actions = {
                    IconButton(onClick = onNavigateToEditProfile) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = LTAThemeColors.TextPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(LTAThemeColors.CardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.username?.take(1) ?: "U").uppercase(),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = LTAThemeColors.PrimaryGold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentUser?.username ?: "Username",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = LTAThemeColors.TextPrimary
                    )

                    val effectiveTeamId = FavoriteTeamCache.getFavoriteTeam() ?: currentUser?.favoriteTeamId

                    if (effectiveTeamId != null) {
                        val teamItem = editProfileUiState.availableTeams.find { it.id == effectiveTeamId }

                        val displayTeam = teamItem ?: createTemporaryTeamItem(effectiveTeamId)

                        key(effectiveTeamId + forceUpdate) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = LTAThemeColors.CardBackground,
                                border = BorderStroke(2.dp, teamColors[effectiveTeamId] ?: LTAThemeColors.PrimaryGold),
                                modifier = Modifier.clickable(onClick = onNavigateToEditProfile)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    LogoImage(
                                        imageUrl = displayTeam.imageUrl,
                                        name = displayTeam.name,
                                        code = displayTeam.code
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = stringResource(R.string.team, displayTeam.code),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = LTAThemeColors.TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        LaunchedEffect(effectiveTeamId) {
                            if (editProfileUiState.availableTeams.isEmpty()) {
                                editProfileViewModel.loadAvailableTeams()
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LTAThemeColors.TextSecondary
                    )

                    val showFeedButton = uiState.friends.isNotEmpty() || currentUser?.favoriteTeamId != null
                    if (showFeedButton) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onViewFriendsFeed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LTAThemeColors.PrimaryGold
                            ),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.view_feed))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                FriendRequestsSection(
                    uiState = requestsUiState,
                    onAcceptRequest = friendsViewModel::acceptFriendRequest,
                    onRejectRequest = friendsViewModel::rejectFriendRequest,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                FriendsSection(
                    uiState = friendsUiState,
                    showFriendsSection = showFriendsSection,
                    onToggleFriendsSection = { showFriendsSection = !showFriendsSection },
                    onFriendUsernameChange = friendsViewModel::updateFriendUsername,
                    onAddFriend = friendsViewModel::sendFriendRequest,
                    onRemoveFriend = friendsViewModel::removeFriend
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                ProfileOptionsSection(
                    onNavigateToMatchHistory = onNavigateToMatchHistory,
                    onNavigateToRanking = onNavigateToRanking,
                    onNavigateToNotificationSettings = onNavigateToNotificationSettings
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Button(
                    onClick = {
                        authViewModel.signOut()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.sign_out))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun createTemporaryTeamItem(teamId: String): TeamFilterItem {
    val code = when (teamId) {
        "loud" -> "LOUD"
        "pain-gaming" -> "PAIN"
        "isurus-estral" -> "IE"
        "leviatan" -> "LEV"
        "furia" -> "FUR"
        "keyd" -> "VKS"
        "red" -> "RED"
        "fxw7" -> "FXW7"
        else -> teamId.take(3).uppercase()
    }

    return TeamFilterItem(
        id = teamId,
        name = teamId.split("-").joinToString(" ") { it.capitalize() },
        code = code,
        imageUrl = ""
    )
}

@Composable
fun ProfileOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = LTAThemeColors.CardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LTAThemeColors.PrimaryGold,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = LTAThemeColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LTAThemeColors.TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = LTAThemeColors.TextSecondary
            )
        }
    }
}

@Composable
fun FriendsSection(
    uiState: FriendsManagementUiState,
    showFriendsSection: Boolean,
    onToggleFriendsSection: () -> Unit,
    onFriendUsernameChange: (String) -> Unit,
    onAddFriend: () -> Unit,
    onRemoveFriend: (Friendship) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LTAThemeColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleFriendsSection)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = LTAThemeColors.PrimaryGold,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.friends),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LTAThemeColors.TextPrimary
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = if (showFriendsSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showFriendsSection) "Esconder amigos" else "Mostrar amigos",
                    tint = LTAThemeColors.TextSecondary
                )
            }

            AnimatedVisibility(
                visible = showFriendsSection,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    AddFriendField(
                        username = uiState.friendUsername,
                        onUsernameChange = onFriendUsernameChange,
                        onAddFriend = onAddFriend,
                        isLoading = uiState.isLoading
                    )

                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (uiState.success != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.success,
                            color = LTAThemeColors.Success,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.friends_count, uiState.friends.size),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = LTAThemeColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.friends.isEmpty()) {
                        EmptyFriendsList()
                    } else {
                        FriendsList(
                            friends = uiState.friends,
                            onRemoveFriend = onRemoveFriend
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddFriendField(
    username: String,
    onUsernameChange: (String) -> Unit,
    onAddFriend: () -> Unit,
    isLoading: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text(stringResource(R.string.username_add)) },
            placeholder = { Text(stringResource(R.string.username_add_placeholder)) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = LTAThemeColors.TextSecondary
                )
            },
            modifier = Modifier.weight(1f),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LTAThemeColors.PrimaryGold,
                unfocusedBorderColor = LTAThemeColors.TextSecondary,
                focusedLabelColor = LTAThemeColors.PrimaryGold,
                unfocusedLabelColor = LTAThemeColors.TextSecondary,
                cursorColor = LTAThemeColors.PrimaryGold,
                focusedTextColor = LTAThemeColors.TextPrimary,
                unfocusedTextColor = LTAThemeColors.TextPrimary
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onAddFriend,
            enabled = username.isNotEmpty() && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = LTAThemeColors.PrimaryGold,
                disabledContainerColor = LTAThemeColors.PrimaryGold.copy(alpha = 0.5f)
            ),
            modifier = Modifier.height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar solicitação",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun EmptyFriendsList() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                color = LTAThemeColors.DarkBackground,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_friends),
            style = MaterialTheme.typography.bodyMedium,
            color = LTAThemeColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FriendsList(
    friends: List<Friendship>,
    onRemoveFriend: (Friendship) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = LTAThemeColors.DarkBackground,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        friends.forEach { friendship ->
            FriendItem(
                friendship = friendship,
                onRemove = { onRemoveFriend(friendship) }
            )
        }
    }
}

@Composable
fun FriendItem(
    friendship: Friendship,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = LTAThemeColors.CardBackground,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(LTAThemeColors.PrimaryGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friendship.friendUsername.take(1).uppercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = LTAThemeColors.PrimaryGold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = friendship.friendUsername,
                style = MaterialTheme.typography.bodyLarge,
                color = LTAThemeColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remover amigo",
                    tint = LTAThemeColors.SecondaryRed
                )
            }
        }
    }
}

@Composable
fun ProfileOptionsSection(
    onNavigateToMatchHistory: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LTAThemeColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.activities),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LTAThemeColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ProfileOptionItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.notification_settings),
                subtitle = stringResource(R.string.notification_settings_desc),
                onClick = onNavigateToNotificationSettings
            )

            ProfileOptionItem(
                icon = Icons.Default.Person,
                title = stringResource(R.string.vote_history),
                subtitle = stringResource(R.string.vote_history_subtitle),
                onClick = onNavigateToMatchHistory
            )

            Divider(color = LTAThemeColors.DarkBackground, thickness = 1.dp)

            ProfileOptionItem(
                icon = Icons.Default.Leaderboard,
                title = stringResource(R.string.player_ranking),
                subtitle = stringResource(R.string.ranking_subtitle),
                onClick = onNavigateToRanking
            )
        }
    }
}