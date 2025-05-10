package com.guicarneirodev.ltascore.android.ui.matches

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.R
import com.guicarneirodev.ltascore.android.viewmodels.MatchFilter
import com.guicarneirodev.ltascore.android.viewmodels.MatchesViewModel
import com.guicarneirodev.ltascore.data.datasource.static.TeamLogoMapper
import com.guicarneirodev.ltascore.domain.models.MatchState
import com.guicarneirodev.ltascore.domain.repository.AdminRepository
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject
import androidx.core.net.toUri
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    viewModel: MatchesViewModel = koinViewModel(),
    onMatchClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onRankingClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isAdmin by remember { mutableStateOf(false) }

    val userRepository: UserRepository = koinInject()
    val adminRepository: AdminRepository = koinInject()

    LaunchedEffect(Unit) {
        try {
            val currentUser = userRepository.getCurrentUser().first()
            if (currentUser != null) {
                val userId = currentUser.id
                android.util.Log.d("AdminCheck", "Verificando admin para usuário $userId")

                adminRepository.isUserAdmin(userId).collectLatest { admin ->
                    android.util.Log.d("AdminCheck", "Status de admin recebido: $admin")
                    isAdmin = admin
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminCheck", "Erro: ${e.message}")
        }
    }

    var showVodDialog by remember { mutableStateOf(false) }
    var currentMatchId by remember { mutableStateOf<String?>(null) }
    var isLoadingVod by remember { mutableStateOf(false) }

    val handleWatchVodClick = { matchId: String ->
        currentMatchId = matchId
        showVodDialog = true
    }

    if (showVodDialog && currentMatchId != null) {
        AlertDialog(
            onDismissRequest = {
                showVodDialog = false
                currentMatchId = null
            },
            title = { Text(stringResource(R.string.exit_app_title)) },
            text = { Text(stringResource(R.string.exit_app_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        isLoadingVod = true
                        showVodDialog = false

                        viewModel.getVodUrlForMatch(currentMatchId!!) { vodUrl ->
                            isLoadingVod = false
                            if (vodUrl != null) {
                                val intent = Intent(Intent.ACTION_VIEW, vodUrl.toUri())
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Vídeo não disponível no momento",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showVodDialog = false
                        currentMatchId = null
                    }
                ) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

    if (isLoadingVod) {
        AlertDialog(
            onDismissRequest = { isLoadingVod = false },
            title = { Text("Carregando") },
            text = { Text("Buscando o vídeo da partida...") },
            confirmButton = {}
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loadMatches()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LTAThemeColors.DarkBackground)
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(TeamLogoMapper.getLtaCrossLogo())
                            .crossfade(true)
                            .build(),
                        contentDescription = "LTA Cross Logo",
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color.White),
                        loading = {
                            Spacer(modifier = Modifier.size(40.dp))
                        },
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = stringResource(R.string.matches_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = LTAThemeColors.PrimaryGold,
                titleContentColor = Color.White
            ),
            actions = {
                IconButton(onClick = onRankingClick) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "Ver Ranking",
                        tint = Color.White
                    )
                }

                IconButton(onClick = onProfileClick) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Perfil do usuário",
                        tint = Color.White
                    )
                }
            }
        )

        TabRow(
            selectedTabIndex = uiState.selectedLeagueIndex,
            containerColor = Color(0xFF2A2A30),
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedLeagueIndex]),
                    height = 3.dp,
                    color = LTAThemeColors.PrimaryGold
                )
            }
        ) {
            uiState.availableLeagues.forEachIndexed { index, league ->
                Tab(
                    selected = index == uiState.selectedLeagueIndex,
                    onClick = { viewModel.selectLeague(index) },
                    text = {
                        Text(
                            text = when(league.name) {
                                "LTA Sul" -> stringResource(R.string.lta_south)
                                "LTA Norte" -> stringResource(R.string.lta_north)
                                "Circuito Desafiante" -> stringResource(R.string.circuito_desafiante)
                                else -> league.name
                            },
                            fontWeight = if (index == uiState.selectedLeagueIndex) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Text(
            text = stringResource(R.string.split_title),
            style = MaterialTheme.typography.titleMedium,
            color = LTAThemeColors.TextSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .background(LTAThemeColors.DarkBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = uiState.filter == MatchFilter.ALL,
                onClick = { viewModel.setFilter(MatchFilter.ALL) },
                label = { Text(stringResource(R.string.all_matches)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF252530),
                    labelColor = LTAThemeColors.TextSecondary,
                    selectedContainerColor = LTAThemeColors.PrimaryGold.copy(alpha = 0.2f),
                    selectedLabelColor = LTAThemeColors.PrimaryGold
                ),
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = uiState.filter == MatchFilter.UPCOMING,
                onClick = { viewModel.setFilter(MatchFilter.UPCOMING) },
                label = { Text(stringResource(R.string.upcoming)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF252530),
                    labelColor = LTAThemeColors.TextSecondary,
                    selectedContainerColor = LTAThemeColors.Warning.copy(alpha = 0.2f),
                    selectedLabelColor = LTAThemeColors.Warning
                ),
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = uiState.filter == MatchFilter.LIVE,
                onClick = { viewModel.setFilter(MatchFilter.LIVE) },
                label = { Text(stringResource(R.string.live)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF252530),
                    labelColor = LTAThemeColors.TextSecondary,
                    selectedContainerColor = LTAThemeColors.LiveRed.copy(alpha = 0.2f),
                    selectedLabelColor = LTAThemeColors.LiveRed
                ),
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = uiState.filter == MatchFilter.COMPLETED,
                onClick = { viewModel.setFilter(MatchFilter.COMPLETED) },
                label = { Text(stringResource(R.string.completed)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF252530),
                    labelColor = LTAThemeColors.TextSecondary,
                    selectedContainerColor = LTAThemeColors.Success.copy(alpha = 0.2f),
                    selectedLabelColor = LTAThemeColors.Success
                ),
                modifier = Modifier.weight(1f)
            )
        }

        if (isAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Green.copy(alpha = 0.3f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Modo Admin Ativo",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        if (uiState.matches.isNotEmpty() || uiState.isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when(uiState.filter) {
                        MatchFilter.ALL -> stringResource(R.string.all_matches_title)
                        MatchFilter.UPCOMING -> stringResource(R.string.upcoming_matches_title)
                        MatchFilter.LIVE -> stringResource(R.string.live_matches_title)
                        MatchFilter.COMPLETED -> stringResource(R.string.completed_matches_title)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = LTAThemeColors.TextSecondary
                )

                if (!uiState.isLoading) {
                    Text(
                        text = stringResource(R.string.matches_count, uiState.filteredMatches.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = LTAThemeColors.TextSecondary
                    )
                }
            }
        }

        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color(0xFF333340)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LTAThemeColors.DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = LTAThemeColors.PrimaryGold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.loading_matches),
                        color = LTAThemeColors.TextSecondary
                    )
                }
            } else if (uiState.error != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Erro",
                        tint = LTAThemeColors.SecondaryRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.error ?: "Erro desconhecido",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = LTAThemeColors.SecondaryRed
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadMatches() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LTAThemeColors.PrimaryGold
                        )
                    ) {
                        Text(stringResource(R.string.try_again))
                    }
                }
            } else if (uiState.filteredMatches.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Informação",
                        tint = LTAThemeColors.TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when(uiState.filter) {
                            MatchFilter.ALL -> stringResource(R.string.no_matches_split)
                            MatchFilter.UPCOMING -> stringResource(R.string.no_upcoming_matches)
                            MatchFilter.LIVE -> stringResource(R.string.no_live_matches)
                            MatchFilter.COMPLETED -> stringResource(R.string.no_completed_matches)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = LTAThemeColors.TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val matchesByDay = uiState.filteredMatches.groupBy { match ->
                        val dateTime = match.startTime.toLocalDateTime(TimeZone.currentSystemDefault())
                        "${dateTime.date.dayOfMonth.toString().padStart(2, '0')}/${dateTime.date.monthNumber.toString().padStart(2, '0')}"
                    }

                    matchesByDay.forEach { (day, matchesOnDay) ->
                        item {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.titleMedium,
                                color = LTAThemeColors.TertiaryGold,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }

                        items(matchesOnDay) { match ->
                            MatchCard(
                                match = match,
                                onClick = { onMatchClick(match.id) },
                                onWatchVodClick = if (match.state == MatchState.COMPLETED && match.hasVod) {
                                    handleWatchVodClick
                                } else null,
                                predictionStats = uiState.matchPredictions[match.id],
                                userPrediction = uiState.userPredictions[match.id],
                                isLoadingPrediction = uiState.predictionsLoading[match.id] == true,
                                onPredictTeam = { teamId ->
                                    viewModel.submitPrediction(match.id, teamId)
                                },
                                weekTitle = if (match.state == MatchState.UNSTARTED) {
                                    stringResource(R.string.week_title, match.blockName)
                                } else {
                                    match.blockName
                                },
                                isAdmin = isAdmin
                            )
                        }
                    }
                }
            }
        }
    }
}