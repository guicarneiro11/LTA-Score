package com.guicarneirodev.ltascore.android.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.guicarneirodev.ltascore.android.viewmodels.MatchSummaryViewModel
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.Player
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.VoteSummary
import org.koin.androidx.compose.koinViewModel

/**
 * Tela que mostra o resumo das avaliações dos jogadores de uma partida
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSummaryScreen(
    viewModel: MatchSummaryViewModel = koinViewModel(),
    matchId: String,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(matchId) {
        viewModel.loadMatch(matchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avaliações da Partida") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
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
                // Cabeçalho com informações da partida
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

                // Contador de votos totais
                val totalVotes = uiState.voteSummaries.sumOf { it.totalVotes }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Total de $totalVotes votos computados",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Divider()

                // Lista de jogadores com suas avaliações
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Time 1
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

                    // Time 2
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

                    // Mensagem para votos do usuário
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
                                        text = "Você já votou nesta partida!",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Obrigado por contribuir com sua avaliação.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        } else {
                            // Se o usuário não votou ainda, não mostra nada nesta tela
                            // (ele deveria estar na tela de votação)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerRatingSummaryItem(
    player: Player,
    averageRating: Double,
    totalVotes: Int
) {
    val ratingColor = when {
        averageRating < 3.0 -> Color(0xFFE57373) // Vermelho claro
        averageRating < 5.0 -> Color(0xFFFFB74D) // Laranja claro
        averageRating < 7.0 -> Color(0xFFFFD54F) // Amarelo
        averageRating < 9.0 -> Color(0xFF81C784) // Verde claro
        else -> Color(0xFF4CAF50) // Verde
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
            // Foto do jogador
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

            // Informações do jogador
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
                    // Badge de posição
                    PositionBadge(position = player.position)

                    Text(
                        text = "${totalVotes} votos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Nota média
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Nota média em destaque
                Text(
                    text = String.format("%.1f", averageRating),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ratingColor
                )

                // Barra de rating visual
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
        else -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
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
        // Time 1
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

        // Placar
        Text(
            text = "$team1Score - $team2Score",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Time 2
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