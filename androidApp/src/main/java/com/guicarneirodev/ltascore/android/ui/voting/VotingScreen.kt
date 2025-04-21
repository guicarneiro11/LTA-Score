package com.guicarneirodev.ltascore.android.ui.voting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guicarneirodev.ltascore.android.viewmodels.VotingViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(
    viewModel: VotingViewModel = koinViewModel(),
    matchId: String,
    onBackClick: () -> Unit,
    onVoteSubmitted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            onVoteSubmitted()
        }
    }

    LaunchedEffect(matchId) {
        viewModel.loadMatch(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avaliar Jogadores") },
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
                Text("Partida não encontrada")
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

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Text(
                            text = uiState.match!!.teams[0].name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    item {
                        Text(
                            text = "Jogadores: ${uiState.match!!.teams[0].players.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (uiState.match!!.teams[0].players.isEmpty()) {
                        item {
                            Text(
                                text = "Nenhum jogador encontrado para este time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(uiState.match!!.teams[0].players) { player ->
                            PlayerVotingItem(
                                player = player,
                                currentRating = uiState.ratings[player.id] ?: 0,
                                onRatingChanged = { rating ->
                                    viewModel.updateRating(player.id, rating)
                                }
                            )
                        }
                    }

                    item {
                        Text(
                            text = uiState.match!!.teams[1].name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }

                    item {
                        Text(
                            text = "Jogadores: ${uiState.match!!.teams[1].players.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (uiState.match!!.teams[1].players.isEmpty()) {
                        item {
                            Text(
                                text = "Nenhum jogador encontrado para este time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(uiState.match!!.teams[1].players) { player ->
                            PlayerVotingItem(
                                player = player,
                                currentRating = uiState.ratings[player.id] ?: 0,
                                onRatingChanged = { rating ->
                                    viewModel.updateRating(player.id, rating)
                                }
                            )
                        }
                    }

                    item {
                        uiState.match!!.teams[0].players.isNotEmpty() ||
                                uiState.match!!.teams[1].players.isNotEmpty()

                        Button(
                            onClick = { viewModel.submitAllRatings() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            enabled = uiState.allPlayersRated && !uiState.isSubmitting
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text("Enviar Avaliações")
                            }
                        }

                        if (uiState.error != null) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}