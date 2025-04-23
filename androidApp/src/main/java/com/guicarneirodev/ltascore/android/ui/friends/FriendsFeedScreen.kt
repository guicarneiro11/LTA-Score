package com.guicarneirodev.ltascore.android.ui.friends

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.guicarneirodev.ltascore.android.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.ui.friends.social.CommentSection
import com.guicarneirodev.ltascore.android.ui.friends.social.ReactionBar
import com.guicarneirodev.ltascore.android.viewmodels.FriendsFeedViewModel
import com.guicarneirodev.ltascore.android.viewmodels.VoteReactionsState
import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.TeamFeedItem
import com.guicarneirodev.ltascore.domain.models.VoteComment
import com.guicarneirodev.ltascore.domain.models.VoteReaction
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsFeedScreen(
    viewModel: FriendsFeedViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    rememberCoroutineScope()
    val tabs = listOf(stringResource(R.string.friends_tab), stringResource(R.string.fans_tab))

    DisposableEffect(Unit) {
        onDispose {
            println("FriendsFeedScreen sendo disposta, mas mantendo listeners ativos")
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.feed_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LTAThemeColors.PrimaryGold,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = {
                            if (uiState.activeTab == 0) {
                                viewModel.loadFriendsFeed()
                            } else {
                                viewModel.loadTeamFeed()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Atualizar",
                                tint = Color.White
                            )
                        }
                    }
                )

                // Abas
                TabRow(
                    selectedTabIndex = uiState.activeTab,
                    containerColor = LTAThemeColors.PrimaryGold,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        if (uiState.activeTab < tabPositions.size) {
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab]),
                                height = 3.dp,
                                color = Color.White
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.activeTab == index,
                            onClick = { viewModel.setActiveTab(index) },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (uiState.activeTab == index)
                                        FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selectedContentColor = Color.White,
                            unselectedContentColor = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LTAThemeColors.DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = LTAThemeColors.PrimaryGold)
            } else if (uiState.error != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = uiState.error ?: "Erro desconhecido",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (uiState.activeTab == 0) {
                                viewModel.loadFriendsFeed()
                            } else {
                                viewModel.loadTeamFeed()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.try_again))
                    }
                }
            } else {
                when (uiState.activeTab) {
                    0 -> {
                        // Aba de Amigos
                        if (uiState.feed.isEmpty()) {
                            EmptyFriendsFeedContent()
                        } else {
                            FriendsFeedContent(
                                groupedFeed = uiState.groupedFeed,
                                voteReactions = uiState.voteReactions,
                                voteComments = uiState.voteComments,
                                currentUserId = uiState.currentUserId,
                                onAddReaction = { voteId, reaction -> viewModel.addReaction(voteId, reaction) },
                                onRemoveReaction = { voteId -> viewModel.removeReaction(voteId) },
                                onAddComment = { voteId, text -> viewModel.addComment(voteId, text) },
                                onDeleteComment = { commentId -> viewModel.deleteComment(commentId) }
                            )
                        }
                    }
                    1 -> {
                        if (uiState.currentUserTeamId == null) {
                            NoTeamSelectedContent(
                                onEditProfileClick = { /* Navegar para tela de edição de perfil */ }
                            )
                        } else if (uiState.teamFeed.isEmpty()) {
                            EmptyTeamFeedContent()
                        } else {
                            TeamFeedContent(
                                groupedFeed = uiState.groupedTeamFeed,
                                voteReactions = uiState.teamVoteReactions,
                                voteComments = uiState.teamVoteComments,
                                currentUserId = uiState.currentUserId,
                                onAddReaction = { voteId, reaction ->
                                    viewModel.addReactionToTeamVote(voteId, reaction)
                                },
                                onRemoveReaction = { voteId ->
                                    viewModel.removeReactionFromTeamVote(voteId)
                                },
                                onAddComment = { voteId, text ->
                                    viewModel.addCommentToTeamVote(voteId, text)
                                },
                                onDeleteComment = { commentId ->
                                    viewModel.deleteCommentFromTeamVote(commentId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTeamFeedContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = LTAThemeColors.TextSecondary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.no_activity),
            style = MaterialTheme.typography.titleMedium,
            color = LTAThemeColors.TextPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.no_votes_display),
            style = MaterialTheme.typography.bodyMedium,
            color = LTAThemeColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NoTeamSelectedContent(onEditProfileClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = LTAThemeColors.TextSecondary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.no_team_selected),
            style = MaterialTheme.typography.titleMedium,
            color = LTAThemeColors.TextPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.select_team_message),
            style = MaterialTheme.typography.bodyMedium,
            color = LTAThemeColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onEditProfileClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = LTAThemeColors.PrimaryGold
            )
        ) {
            Text(stringResource(R.string.choose_team_button))
        }
    }
}

@Composable
fun TeamFeedContent(
    groupedFeed: Map<String, List<TeamFeedItem>>,
    voteReactions: Map<String, VoteReactionsState>,
    voteComments: Map<String, List<VoteComment>>,
    currentUserId: String,
    onAddReaction: (String, String) -> Unit,
    onRemoveReaction: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedFeed.forEach { (groupKey, votes) ->
            val parts = groupKey.split(":")
            val username = parts[0].trim()
            val matchInfo = parts.getOrNull(1)?.trim()?.split("|") ?: listOf("", "")
            val dateStr = matchInfo.getOrNull(1) ?: ""

            item(key = "header_$groupKey") {
                TeamFeedHeader(
                    username = username,
                    date = dateStr,
                    teams = "${votes.firstOrNull()?.teamCode ?: ""} vs ${votes.firstOrNull()?.opponentTeamCode ?: ""}",
                    isCurrentUser = votes.firstOrNull()?.userId == currentUserId,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(votes, key = { it.id }) { vote ->
                val reactionsState = voteReactions[vote.id] ?: VoteReactionsState()
                val commentsList = voteComments[vote.id] ?: emptyList()

                TeamVoteItem(
                    vote = vote,
                    reactions = reactionsState.reactions,
                    userReaction = reactionsState.userReaction,
                    comments = commentsList,
                    currentUserId = currentUserId,
                    isCurrentUser = vote.userId == currentUserId,
                    onAddReaction = { reaction -> onAddReaction(vote.id, reaction) },
                    onRemoveReaction = { onRemoveReaction(vote.id) },
                    onAddComment = { text -> onAddComment(vote.id, text) },
                    onDeleteComment = onDeleteComment
                )
            }

            item(key = "divider_$groupKey") {
                Divider(
                    color = Color(0xFF333340),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun TeamFeedHeader(
    modifier: Modifier = Modifier,
    username: String,
    date: String,
    teams: String,
    isCurrentUser: Boolean = false
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentUser) LTAThemeColors.TertiaryGold.copy(alpha = 0.3f)
                        else LTAThemeColors.PrimaryGold.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.take(1).uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentUser) LTAThemeColors.TertiaryGold else LTAThemeColors.PrimaryGold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (isCurrentUser) "$username (you)" else username,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrentUser) LTAThemeColors.TertiaryGold else LTAThemeColors.PrimaryGold,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = date,
            style = MaterialTheme.typography.labelMedium,
            color = LTAThemeColors.TextSecondary
        )

        Text(
            text = teams,
            style = MaterialTheme.typography.titleMedium,
            color = LTAThemeColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyFriendsFeedContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = LTAThemeColors.TextSecondary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.no_activity),
            style = MaterialTheme.typography.titleMedium,
            color = LTAThemeColors.TextPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.no_votes_display),
            style = MaterialTheme.typography.bodyMedium,
            color = LTAThemeColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(
                containerColor = LTAThemeColors.PrimaryGold
            )
        ) {
            Text(stringResource(R.string.add_friends))
        }
    }
}

@Composable
fun FriendsFeedContent(
    groupedFeed: Map<String, List<FriendVoteHistoryItem>>,
    voteReactions: Map<String, VoteReactionsState>,
    voteComments: Map<String, List<VoteComment>>,
    currentUserId: String,
    onAddReaction: (String, String) -> Unit,
    onRemoveReaction: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    val youText = stringResource(R.string.you)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedFeed.forEach { (groupKey, votes) ->
            val parts = groupKey.split(":")
            val friendName = parts[0].trim()
            val matchInfo = parts.getOrNull(1)?.trim()?.split("|") ?: listOf("", "")
            val dateStr = matchInfo.getOrNull(1) ?: ""

            val isCurrentUser = friendName.contains("(you)") || votes.firstOrNull()?.friendId == currentUserId

            item(key = "header_$groupKey") {
                FeedGroupHeader(
                    friendName = if (isCurrentUser) {
                        friendName.replace("(you)", youText)
                    } else {
                        friendName
                    },
                    date = dateStr,
                    teams = "${votes.firstOrNull()?.teamCode ?: ""} vs ${votes.firstOrNull()?.opponentTeamCode ?: ""}",
                    isCurrentUser = isCurrentUser,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(votes, key = { it.id }) { vote ->
                val reactionsState = voteReactions[vote.id] ?: VoteReactionsState()
                val commentsList = voteComments[vote.id] ?: emptyList()

                val uniqueComments = commentsList.distinctBy { it.id }

                LaunchedEffect(vote.id) {
                    println("Renderizando voto ${vote.id}: ${reactionsState.reactions.size} reações, ${uniqueComments.size} comentários únicos")
                }

                FriendVoteItem(
                    vote = vote,
                    reactions = reactionsState.reactions,
                    userReaction = reactionsState.userReaction,
                    comments = uniqueComments,
                    currentUserId = currentUserId,
                    isCurrentUser = vote.friendId == currentUserId,
                    onAddReaction = { reaction -> onAddReaction(vote.id, reaction) },
                    onRemoveReaction = { onRemoveReaction(vote.id) },
                    onAddComment = { text -> onAddComment(vote.id, text) },
                    onDeleteComment = onDeleteComment
                )
            }

            item(key = "divider_$groupKey") {
                Divider(
                    color = Color(0xFF333340),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun FeedGroupHeader(
    modifier: Modifier = Modifier,
    friendName: String,
    date: String,
    teams: String,
    isCurrentUser: Boolean = false
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentUser) LTAThemeColors.TertiaryGold.copy(alpha = 0.3f)
                        else LTAThemeColors.PrimaryGold.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friendName.take(1).uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentUser) LTAThemeColors.TertiaryGold else LTAThemeColors.PrimaryGold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = friendName,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrentUser) LTAThemeColors.TertiaryGold else LTAThemeColors.PrimaryGold,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = date,
            style = MaterialTheme.typography.labelMedium,
            color = LTAThemeColors.TextSecondary
        )

        Text(
            text = teams,
            style = MaterialTheme.typography.titleMedium,
            color = LTAThemeColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun TeamVoteItem(
    modifier: Modifier = Modifier,
    vote: TeamFeedItem,
    reactions: List<VoteReaction>,
    userReaction: VoteReaction?,
    comments: List<VoteComment>,
    currentUserId: String,
    isCurrentUser: Boolean = false,
    onAddReaction: (String) -> Unit,
    onRemoveReaction: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    val ratingColor = when {
        vote.rating < 3.0f -> Color(0xFFE57373)
        vote.rating < 5.0f -> Color(0xFFFFB74D)
        vote.rating < 7.0f -> Color(0xFFFFD54F)
        vote.rating < 9.0f -> Color(0xFF81C784)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser)
                LTAThemeColors.CardBackground.copy(alpha = 0.95f)
                    .compositeOver(LTAThemeColors.TertiaryGold.copy(alpha = 0.1f))
            else
                LTAThemeColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (isCurrentUser)
            BorderStroke(1.dp, LTAThemeColors.TertiaryGold.copy(alpha = 0.3f))
        else
            null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Imagem do jogador
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(vote.playerImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = vote.playerNickname,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                // Informações do jogador
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = vote.playerNickname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LTAThemeColors.TextPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        PositionBadge(position = vote.playerPosition)

                        Spacer(modifier = Modifier.width(8.dp))

                        // Mostrar quando foi compartilhado
                        Text(
                            text = stringResource(R.string.shared_at, formatTime(vote.sharedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = LTAThemeColors.TextSecondary
                        )
                    }
                }

                // Nota do jogador
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = String.format("%.1f", vote.rating),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ratingColor
                    )

                    // Barra visual da nota
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(60.dp * (vote.rating / 10f))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(ratingColor.copy(alpha = 0.7f), ratingColor)
                                    ),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }

            // Destaque visual se for o voto do usuário atual
            if (isCurrentUser) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(LTAThemeColors.TertiaryGold.copy(alpha = 0.3f))
                )
            }

            // Seção de reações
            ReactionBar(
                voteId = vote.id,
                reactions = reactions,
                userReaction = userReaction,
                onReactionSelected = onAddReaction,
                onReactionRemoved = onRemoveReaction
            )

            // Separador entre reações e comentários
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 16.dp)
                .background(LTAThemeColors.DarkBackground)
            )

            // Seção de comentários
            CommentSection(
                voteId = vote.id,
                comments = comments,
                currentUserId = currentUserId,
                onAddComment = onAddComment,
                onDeleteComment = onDeleteComment
            )
        }
    }
}

@Composable
private fun formatTime(timestamp: Instant): String {
    val now = Clock.System.now()
    val diff = now - timestamp

    return when {
        diff.inWholeSeconds < 60 -> stringResource(R.string.now)
        diff.inWholeMinutes < 60 -> stringResource(R.string.mins_ago, diff.inWholeMinutes)
        diff.inWholeHours < 24 -> stringResource(R.string.hours_ago, diff.inWholeHours)
        diff.inWholeDays < 7 -> stringResource(R.string.days_ago, diff.inWholeDays)
        else -> {
            val date = timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
            stringResource(R.string.date_format, date.dayOfMonth, date.monthNumber)
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun FriendVoteItem(
    modifier: Modifier = Modifier,
    vote: FriendVoteHistoryItem,
    reactions: List<VoteReaction>,
    userReaction: VoteReaction?,
    comments: List<VoteComment>,
    currentUserId: String,
    isCurrentUser: Boolean = false,
    onAddReaction: (String) -> Unit,
    onRemoveReaction: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    val ratingColor = when {
        vote.rating < 3.0f -> Color(0xFFE57373)
        vote.rating < 5.0f -> Color(0xFFFFB74D)
        vote.rating < 7.0f -> Color(0xFFFFD54F)
        vote.rating < 9.0f -> Color(0xFF81C784)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser)
                LTAThemeColors.CardBackground.copy(alpha = 0.95f)
                    .compositeOver(LTAThemeColors.TertiaryGold.copy(alpha = 0.1f))
            else
                LTAThemeColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (isCurrentUser)
            BorderStroke(1.dp, LTAThemeColors.TertiaryGold.copy(alpha = 0.3f))
        else
            null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(vote.playerImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = vote.playerNickname,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = vote.playerNickname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LTAThemeColors.TextPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        PositionBadge(position = vote.playerPosition)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = String.format("%.1f", vote.rating),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ratingColor
                    )

                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(60.dp * (vote.rating.toFloat() / 10f))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(ratingColor.copy(alpha = 0.7f), ratingColor)
                                    ),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }

            if (isCurrentUser) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(LTAThemeColors.TertiaryGold.copy(alpha = 0.3f))
                )
            }

            ReactionBar(
                voteId = vote.id,
                reactions = reactions,
                userReaction = userReaction,
                onReactionSelected = onAddReaction,
                onReactionRemoved = onRemoveReaction
            )

            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 16.dp)
                .background(LTAThemeColors.DarkBackground)
            )

            CommentSection(
                voteId = vote.id,
                comments = comments,
                currentUserId = currentUserId,
                onAddComment = onAddComment,
                onDeleteComment = onDeleteComment
            )
        }
    }
}

@Composable
fun PositionBadge(position: PlayerPosition) {
    val (backgroundColor, textColor) = when(position) {
        PlayerPosition.TOP -> Color(0xFF3498db) to Color.White
        PlayerPosition.JUNGLE -> Color(0xFF2ecc71) to Color.White
        PlayerPosition.MID -> Color(0xFFe74c3c) to Color.White
        PlayerPosition.ADC -> Color(0xFFf39c12) to Color.White
        PlayerPosition.SUPPORT -> Color(0xFF9b59b6) to Color.White
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor,
        modifier = Modifier.height(20.dp)
    ) {
        Text(
            text = position.name,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}