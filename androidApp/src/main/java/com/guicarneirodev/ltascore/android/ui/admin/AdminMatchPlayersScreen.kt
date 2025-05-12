package com.guicarneirodev.ltascore.android.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.viewmodels.AdminViewModel
import com.guicarneirodev.ltascore.domain.models.MatchState
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMatchPlayersScreen(
    viewModel: AdminViewModel = koinViewModel(),
    matchId: String,
    onBackClick: () -> Unit,
    onNavigateToMatchSummary: (String) -> Unit = {},
    onNavigateToVoting: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSuccessMessage by remember { mutableStateOf(false) }

    LaunchedEffect(matchId) {
        viewModel.loadMatch(matchId)
    }

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            showSuccessMessage = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Jogadores da Partida") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.error != null && uiState.match == null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.error ?: "Erro desconhecido",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadMatch(matchId) }) {
                        Text("Tentar Novamente")
                    }
                }
            } else if (uiState.match != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Mostrar informações da partida
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
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
                                    model = uiState.match!!.teams[0].imageUrl,
                                    contentDescription = uiState.match!!.teams[0].code,
                                    modifier = Modifier.size(50.dp)
                                )
                                Text(
                                    text = uiState.match!!.teams[0].code,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "vs",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = uiState.match!!.teams[1].imageUrl,
                                    contentDescription = uiState.match!!.teams[1].code,
                                    modifier = Modifier.size(50.dp)
                                )
                                Text(
                                    text = uiState.match!!.teams[1].code,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Selecione os jogadores que participaram desta partida:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Agrupar jogadores por time
                    val playersByTeam = uiState.allPlayers.groupBy { player ->
                        uiState.match!!.teams.find { team -> team.id == player.teamId }?.name ?: "Time Desconhecido"
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        playersByTeam.forEach { (teamName, players) ->
                            item {
                                Text(
                                    text = teamName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(players) { player ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(
                                            if (uiState.selectedPlayerIds.contains(player.id))
                                                LTAThemeColors.PrimaryGold.copy(alpha = 0.1f)
                                            else
                                                Color.Transparent
                                        )
                                        .padding(8.dp)
                                ) {
                                    Checkbox(
                                        checked = uiState.selectedPlayerIds.contains(player.id),
                                        onCheckedChange = { isChecked ->
                                            viewModel.updatePlayerSelection(player.id, isChecked)
                                        }
                                    )

                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(player.imageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = player.nickname,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(MaterialTheme.shapes.small)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .weight(1f)
                                    ) {
                                        Text(
                                            text = player.nickname,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val (backgroundColor, textColor) = when(player.position) {
                                                PlayerPosition.TOP -> Color(0xFF3498db) to Color.White
                                                PlayerPosition.JUNGLE -> Color(0xFF2ecc71) to Color.White
                                                PlayerPosition.MID -> Color(0xFFe74c3c) to Color.White
                                                PlayerPosition.ADC -> Color(0xFFf39c12) to Color.White
                                                PlayerPosition.SUPPORT -> Color(0xFF9b59b6) to Color.White
                                            }

                                            Surface(
                                                shape = MaterialTheme.shapes.extraSmall,
                                                color = backgroundColor,
                                                modifier = Modifier.height(20.dp)
                                            ) {
                                                Text(
                                                    text = player.position.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = textColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = player.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }

                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.saveParticipatingPlayers(matchId)
                            if (!uiState.isSubmitting) {
                                onNavigateToVoting(matchId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Salvar e Gerenciar Votação")
                        }
                    }

                    if (uiState.match?.state == MatchState.COMPLETED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onNavigateToMatchSummary(matchId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ver Resumo da Partida")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onNavigateToVoting(matchId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ir para Votação")
                        }
                    }
                }
            }

            if (showSuccessMessage) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { showSuccessMessage = false }) {
                            Text("OK")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jogadores salvos com sucesso!")
                    }
                }
            }
        }
    }
}