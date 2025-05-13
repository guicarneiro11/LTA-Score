package com.guicarneirodev.ltascore.android.ui.summary

import android.annotation.SuppressLint
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.guicarneirodev.ltascore.android.viewmodels.MatchSummaryViewModel
import com.guicarneirodev.ltascore.domain.models.Player
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import org.koin.androidx.compose.koinViewModel
import com.guicarneirodev.ltascore.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSummaryScreen(
    viewModel: MatchSummaryViewModel = koinViewModel(),
    matchId: String,
    onBackClick: () -> Unit,
    onAdminClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAdminButton by remember { mutableStateOf(false) }

    LaunchedEffect(matchId) {
        viewModel.loadMatch(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.match_ratings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (showAdminButton) {
                        IconButton(onClick = { onAdminClick(matchId) }) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Gerenciar Jogadores",
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.loadMatch(matchId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.loading_ratings),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        else if (uiState.error != null || uiState.match == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Erro",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.error ?: stringResource(R.string.match_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.loadMatch(matchId) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.try_again))
                    }
                }
            }
        }
        else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                MatchHeader(
                    team1Name = uiState.match!!.teams[0].name,
                    team1Code = uiState.match!!.teams[0].code,
                    team1Logo = uiState.match!!.teams[0].imageUrl,
                    team1Score = uiState.match!!.teams[0].result.gameWins,
                    team2Name = uiState.match!!.teams[1].name,
                    team2Code = uiState.match!!.teams[1].code,
                    team2Logo = uiState.match!!.teams[1].imageUrl,
                    team2Score = uiState.match!!.teams[1].result.gameWins,
                )

                val totalVotes = uiState.voteSummaries.sumOf { it.totalVotes }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (totalVotes > 0)
                            stringResource(R.string.total_votes, totalVotes)
                        else
                            stringResource(R.string.waiting_first_vote),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Divider()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Text(
                            text = uiState.match!!.teams[0].name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(uiState.match!!.teams[0].players) { player ->
                        val summary = uiState.voteSummaries.find { it.playerId == player.id }
                        PlayerRatingSummaryItem(
                            player = player,
                            averageRating = summary?.averageRating ?: 0.0,
                            totalVotes = summary?.totalVotes ?: 0
                        )
                    }

                    item {
                        Text(
                            text = uiState.match!!.teams[1].name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }

                    items(uiState.match!!.teams[1].players) { player ->
                        val summary = uiState.voteSummaries.find { it.playerId == player.id }
                        PlayerRatingSummaryItem(
                            player = player,
                            averageRating = summary?.averageRating ?: 0.0,
                            totalVotes = summary?.totalVotes ?: 0
                        )
                    }

                    item {
                        if (uiState.userHasVoted) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.already_voted),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.thanks_voting),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.not_voted_yet),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.vote_instruction),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun PlayerRatingSummaryItem(
    player: Player,
    averageRating: Double,
    totalVotes: Int
) {
    val ratingColor = when {
        averageRating < 3.0 -> Color(0xFFE57373)
        averageRating < 5.0 -> Color(0xFFFFB74D)
        averageRating < 7.0 -> Color(0xFFFFD54F)
        averageRating < 9.0 -> Color(0xFF81C784)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                model = ImageRequest.Builder(LocalContext.current)
                    .data(player.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = player.nickname,
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
                    text = player.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    PositionBadge(position = player.position)

                    Text(
                        text = stringResource(R.string.votes_count, totalVotes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }


            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format("%.1f", averageRating),
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
                            .width(60.dp * (averageRating.toFloat() / 10f))
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

@Composable
fun MatchHeader(
    team1Name: String,
    team1Code: String,
    team1Logo: String,
    team1Score: Int,
    team2Name: String,
    team2Code: String,
    team2Logo: String,
    team2Score: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            AsyncImage(
                model = team1Logo,
                contentDescription = team1Name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(60.dp)
            )
            Text(
                text = team1Code,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "$team1Score - $team2Score",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            AsyncImage(
                model = team2Logo,
                contentDescription = team2Name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(60.dp)
            )
            Text(
                text = team2Code,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}