package com.guicarneirodev.ltascore.android.ui.voting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guicarneirodev.ltascore.android.R
import com.guicarneirodev.ltascore.android.ui.admin.AdminPlayerVotingItem
import com.guicarneirodev.ltascore.android.viewmodels.VotingViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(
    viewModel: VotingViewModel = koinViewModel(),
    matchId: String,
    isAdmin: Boolean = false,
    onBackClick: () -> Unit,
    onVoteSubmitted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(matchId, isAdmin) {
        viewModel.loadMatch(matchId, isAdmin)
    }

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            onVoteSubmitted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isAdmin) {
                        Text("Gerenciar Jogadores da Partida")
                    } else {
                        Text(stringResource(R.string.rate_players))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
                CircularProgressIndicator()
            }
        } else if (uiState.match == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.match_not_found))
            }
        } else {
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (isAdmin) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E8E3E).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Modo Administrador",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E8E3E)
                            )

                            Text(
                                text = "Selecione os jogadores que participaram desta partida:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                            )

                            Button(
                                onClick = { viewModel.saveParticipatingPlayers() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E8E3E)
                                )
                            ) {
                                Text("Salvar Jogadores Participantes")
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    uiState.match!!.teams.forEachIndexed { _, team ->
                        item {
                            Text(
                                text = team.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        item {
                            Text(
                                text = stringResource(R.string.players_count, team.players.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        if (team.players.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.no_players_found),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        } else {
                            items(team.players) { player ->
                                if (isAdmin) {
                                    AdminPlayerVotingItem(
                                        player = player,
                                        isParticipating = uiState.participatingPlayerIds.contains(player.id),
                                        onParticipationChanged = { isParticipating ->
                                            viewModel.updatePlayerParticipation(player.id, isParticipating)
                                        }
                                    )
                                } else {
                                    PlayerVotingItem(
                                        player = player,
                                        currentRating = uiState.ratings[player.id] ?: 0f,
                                        onRatingChanged = { rating ->
                                            viewModel.updateRating(player.id, rating)
                                        }
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    if (!isAdmin) {
                        item {
                            Button(
                                onClick = { viewModel.submitAllRatings() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                enabled = uiState.allPlayersRated && !uiState.isSubmitting
                            ) {
                                if (uiState.isSubmitting) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(stringResource(R.string.submit_ratings))
                                }
                            }
                        }
                    }

                    if (uiState.error != null) {
                        item {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}