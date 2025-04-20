package com.guicarneirodev.ltascore.android.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.ui.friends.social.FriendRequestsSection
import com.guicarneirodev.ltascore.android.viewmodels.AuthViewModel
import com.guicarneirodev.ltascore.android.viewmodels.FriendsManagementUiState
import com.guicarneirodev.ltascore.android.viewmodels.FriendsViewModel
import com.guicarneirodev.ltascore.domain.models.Friendship
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    friendsViewModel: FriendsViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
    onNavigateToMatchHistory: () -> Unit,
    onNavigateToRanking: () -> Unit,
    onNavigateToFriendsFeed: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onBackClick: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val friendsUiState by friendsViewModel.uiState.collectAsState()
    val requestsUiState by friendsViewModel.requestsUiState.collectAsState()

    // Estado para controlar a visibilidade da seção de amigos
    var showFriendsSection by remember { mutableStateOf(true) }

    // Mapeamento de times para cores
    val teamColors = mapOf(
        "loud" to Color(0xFF33CC33),     // Verde LOUD
        "pain-gaming" to Color(0xFFFFD700), // Dourado paiN
        "isurus-estral" to Color(0xFF0066CC), // Azul IE
        "leviatan" to Color(0xFFCCCCCC),  // Cinza LEVIATÁN
        "furia" to Color(0xFF000000),     // Preto FURIA
        "keyd" to Color(0xFFFFFFFF),      // Branco Keyd
        "red" to Color(0xFFFF0000),       // Vermelho RED
        "fxw7" to Color(0xFF9966CC)       // Roxo Fluxo W7M
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LTAThemeColors.CardBackground,
                    titleContentColor = LTAThemeColors.TextPrimary,
                    navigationIconContentColor = LTAThemeColors.TextPrimary
                ),
                actions = {
                    // Botão de editar perfil
                    IconButton(onClick = onNavigateToEditProfile) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Perfil",
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
            // Item com o cabeçalho do perfil
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar do usuário
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(LTAThemeColors.CardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        // Verificar se há um time favorito para adicionar um indicador visual
                        if (currentUser?.favoriteTeamId != null) {
                            // Cor do time para o contorno do avatar
                            val teamColor = teamColors[currentUser!!.favoriteTeamId] ?: LTAThemeColors.PrimaryGold

                            // Criar um contorno colorido para o avatar com a cor do time
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .border(BorderStroke(3.dp, teamColor), CircleShape)
                            )
                        }

                        Text(
                            text = (currentUser?.username?.take(1) ?: "U").uppercase(),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = LTAThemeColors.PrimaryGold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nome de usuário
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Nome de usuário
                        Text(
                            text = currentUser?.username ?: "Usuário",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = LTAThemeColors.TextPrimary
                        )

                        // Logo do time favorito (se disponível)
                        if (currentUser?.favoriteTeamId != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TeamLogoImage(
                                teamId = currentUser!!.favoriteTeamId,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White, CircleShape)
                                    .padding(2.dp)
                            )
                        }
                    }

                    // NOVA ABORDAGEM: Badge de time favorito
                    if (currentUser?.favoriteTeamId != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Obter o código e a cor do time
                        val teamCode = getTeamCode(currentUser!!.favoriteTeamId)
                        val teamColor = teamColors[currentUser!!.favoriteTeamId] ?: LTAThemeColors.PrimaryGold

                        // Badge de time favorito
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = LTAThemeColors.CardBackground,
                            border = BorderStroke(2.dp, teamColor),
                            modifier = Modifier.clickable(onClick = onNavigateToEditProfile)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                // Miniatura do logo
                                TeamLogoImage(
                                    teamId = currentUser!!.favoriteTeamId,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(Color.White, CircleShape)
                                        .padding(2.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Nome do time
                                Text(
                                    text = "Time: $teamCode",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LTAThemeColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Email
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LTAThemeColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Resto do conteúdo permanece o mesmo
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
                    onRemoveFriend = friendsViewModel::removeFriend,
                    onViewFriendsFeed = onNavigateToFriendsFeed
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                ProfileOptionsSection(
                    onNavigateToMatchHistory = onNavigateToMatchHistory,
                    onNavigateToRanking = onNavigateToRanking
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
                    Text("Sair da Conta")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TeamLogoImage(
    teamId: String?,
    modifier: Modifier = Modifier
) {
    if (teamId == null) return

    // Mapear os IDs dos times para suas URLs de imagem
    val teamImageUrl = when(teamId) {
        "loud" -> "https://static.lolesports.com/teams/1677205962121_1631819715466_loudlogoteams-1.png"
        "pain-gaming" -> "https://static.lolesports.com/teams/1592590486578_PaiN-2020-Yellow.png"
        "isurus-estral" -> "https://static.lolesports.com/teams/1702323368603_ISGLogo_LTA_2024_Grayscale.png"
        "leviatan" -> "https://static.lolesports.com/teams/1673462138550_LVT-2023-1.png"
        "furia" -> "https://static.lolesports.com/teams/furia.png"
        "keyd" -> "https://static.lolesports.com/teams/1631819823314_keydstarsteams1.png"
        "red" -> "https://static.lolesports.com/teams/red-canids-kalunga_-_2021_-_split_2.png"
        "fxw7" -> "https://static.lolesports.com/teams/1704324649444_FluxoW7M.png"
        else -> null
    }

    if (teamImageUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(teamImageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Logo do time favorito",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

// Função auxiliar para obter o código do time
private fun getTeamCode(teamId: String?): String {
    return when(teamId) {
        "loud" -> "LOUD"
        "pain-gaming" -> "PAIN"
        "isurus-estral" -> "IE"
        "leviatan" -> "LEV"
        "furia" -> "FUR"
        "keyd" -> "VKS"
        "red" -> "RED"
        "fxw7" -> "FXW7"
        else -> teamId?.take(3)?.uppercase() ?: ""
    }
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
    onRemoveFriend: (Friendship) -> Unit,
    onViewFriendsFeed: () -> Unit
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
                    text = "Amigos",
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

                if (uiState.friends.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onViewFriendsFeed,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LTAThemeColors.PrimaryGold
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Ver Feed", style = MaterialTheme.typography.labelMedium)
                    }
                }
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
                        text = "Meus Amigos (${uiState.friends.size})",
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
            label = { Text("Nome de usuário") },
            placeholder = { Text("Digite o username para enviar solicitação") },
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
            text = "Você ainda não adicionou nenhum amigo",
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
    onNavigateToRanking: () -> Unit
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
                text = "Atividades",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LTAThemeColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ProfileOptionItem(
                icon = Icons.Default.Person,
                title = "Meu Histórico de Votos",
                subtitle = "Veja suas avaliações de jogadores",
                onClick = onNavigateToMatchHistory
            )

            Divider(color = LTAThemeColors.DarkBackground, thickness = 1.dp)

            ProfileOptionItem(
                icon = Icons.Default.Leaderboard,
                title = "Ranking de Jogadores",
                subtitle = "Veja as avaliações da comunidade",
                onClick = onNavigateToRanking
            )
        }
    }
}