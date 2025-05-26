package com.guicarneirodev.ltascore.android.ui.history

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.guicarneirodev.ltascore.android.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.viewmodels.VoteHistoryViewModel
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.UserVoteHistoryItem
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoteHistoryScreen(
    viewModel: VoteHistoryViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showShareDialog by remember { mutableStateOf(false) }
    var selectedMatchId by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.shareSuccess) {
        if (uiState.shareSuccess != null) {
            snackbarHostState.showSnackbar(
                message = uiState.shareSuccess!!,
                duration = SnackbarDuration.Short
            )
        }
    }

    if (showShareDialog) {
        ShareVoteDialog(
            matchId = selectedMatchId,
            onDismiss = { showShareDialog = false },
            onShareToTeamFeed = { matchId ->
                viewModel.shareVoteToTeamFeed(matchId)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vote_history_title)) },
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
                    IconButton(onClick = { viewModel.loadVoteHistory() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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

                    Button(onClick = { viewModel.loadVoteHistory() }) {
                        Text(stringResource(R.string.try_again))
                    }
                }
            } else if (uiState.history.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_votes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = LTAThemeColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.groupedHistory.forEach { (groupKey, votes) ->
                        val parts = groupKey.split("|")
                        parts[0]
                        val dateStr = if (parts.size > 1) parts[1] else ""

                        item(key = "header_$groupKey") {
                            MatchHeader(
                                date = dateStr,
                                teams = "${votes.first().teamCode} vs ${votes.first().opponentTeamCode}",
                                matchId = votes.first().matchId,
                                onShareClick = { matchId ->
                                    selectedMatchId = matchId
                                    showShareDialog = true
                                },
                                isSharing = uiState.isSharing && selectedMatchId == votes.first().matchId,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(votes, key = { it.id }) { vote ->
                            VoteHistoryItem(vote = vote)
                        }

                        item(key = "divider_$groupKey") {
                            HorizontalDivider(
                                color = Color(0xFF333340),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchHeader(
    modifier: Modifier = Modifier,
    date: String,
    teams: String,
    matchId: String,
    onShareClick: (String) -> Unit,
    isSharing: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium,
                color = LTAThemeColors.TertiaryGold,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = teams,
                style = MaterialTheme.typography.titleLarge,
                color = LTAThemeColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        IconButton(
            onClick = { onShareClick(matchId) },
            enabled = !isSharing
        ) {
            if (isSharing) {
                CircularProgressIndicator(
                    color = LTAThemeColors.PrimaryGold,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Compartilhar",
                    tint = LTAThemeColors.PrimaryGold
                )
            }
        }
    }
}

@Composable
fun ShareVoteDialog(
    matchId: String,
    onDismiss: () -> Unit,
    onShareToTeamFeed: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_votes)) },
        text = {
            Text(stringResource(R.string.share_votes_question))
        },
        confirmButton = {
            Button(
                onClick = {
                    onShareToTeamFeed(matchId)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LTAThemeColors.PrimaryGold
                )
            ) {
                Text(stringResource(R.string.share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = LTAThemeColors.CardBackground,
        titleContentColor = LTAThemeColors.TextPrimary,
        textContentColor = LTAThemeColors.TextPrimary
    )
}

@SuppressLint("DefaultLocale")
@Composable
fun VoteHistoryItem(
    vote: UserVoteHistoryItem,
    modifier: Modifier = Modifier
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
            containerColor = LTAThemeColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = vote.playerImage,
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatTime(vote.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = LTAThemeColors.TextSecondary
                    )
                }
            }

            Text(
                text = String.format("%.1f", vote.rating),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ratingColor
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

private fun formatTime(timestamp: Instant): String {
    val localDateTime = timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour}:${localDateTime.minute.toString().padStart(2, '0')}"
}