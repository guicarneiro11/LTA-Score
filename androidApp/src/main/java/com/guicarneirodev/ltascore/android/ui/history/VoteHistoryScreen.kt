package com.guicarneirodev.ltascore.android.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Votos") },
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

                    Button(onClick = { viewModel.loadVoteHistory() }) {
                        Text("Tentar Novamente")
                    }
                }
            } else if (uiState.history.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Você ainda não votou em nenhuma partida.",
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
                    // Para cada grupo (partida + data)
                    uiState.groupedHistory.forEach { (groupKey, votes) ->
                        val parts = groupKey.split("|")
                        val matchId = parts[0]
                        val dateStr = if (parts.size > 1) parts[1] else ""

                        // Cabeçalho da partida
                        item(key = "header_$groupKey") {
                            MatchHeader(
                                date = dateStr,
                                teams = "${votes.first().teamCode} vs ${votes.first().opponentTeamCode}",
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        // Itens de voto para esta partida
                        items(votes, key = { it.id }) { vote ->
                            VoteHistoryItem(vote = vote)
                        }

                        // Separador entre partidas
                        item(key = "divider_$groupKey") {
                            Divider(
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
    date: String,
    teams: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
}

@Composable
fun VoteHistoryItem(
    vote: UserVoteHistoryItem,
    modifier: Modifier = Modifier
) {
    val ratingColor = when {
        vote.rating < 3.0f -> Color(0xFFE57373) // Vermelho claro
        vote.rating < 5.0f -> Color(0xFFFFB74D) // Laranja claro
        vote.rating < 7.0f -> Color(0xFFFFD54F) // Amarelo
        vote.rating < 9.0f -> Color(0xFF81C784) // Verde claro
        else -> Color(0xFF4CAF50) // Verde
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
            // Foto do jogador
            AsyncImage(
                model = vote.playerImage,
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
                    // Badge de posição
                    PositionBadge(position = vote.playerPosition)

                    Spacer(modifier = Modifier.width(8.dp))

                    // Hora do voto
                    Text(
                        text = formatTime(vote.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = LTAThemeColors.TextSecondary
                    )
                }
            }

            // Nota dada
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