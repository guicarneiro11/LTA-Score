package com.guicarneirodev.ltascore.android.ui.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.viewmodels.RankingViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Tela principal de ranking dos jogadores
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    viewModel: RankingViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Carrega o ranking quando a tela é montada
    LaunchedEffect(Unit) {
        viewModel.loadRanking()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ranking de Jogadores") },
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
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    // Botão de atualizar
                    IconButton(
                        onClick = { viewModel.refreshRanking() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = LTAThemeColors.DarkBackground
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Filtros
                RankingFilters(
                    currentFilter = uiState.filterState.currentFilter,
                    onFilterSelected = viewModel::setFilterType,
                    onTeamSelected = viewModel::selectTeam,
                    onPositionSelected = viewModel::selectPosition,
                    selectedTeamId = uiState.filterState.selectedTeamId,
                    selectedPosition = uiState.filterState.selectedPosition,
                    availableTeams = uiState.availableTeams,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChanged = viewModel::updateSearchQuery
                )

                // Conteúdo principal (lista ou estados de carregamento/erro)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LTAThemeColors.DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    // Estado de carregamento
                    if (uiState.isLoading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = LTAThemeColors.PrimaryGold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Carregando ranking...",
                                color = LTAThemeColors.TextSecondary
                            )
                        }
                    }
                    // Estado de erro
                    else if (uiState.error != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Erro",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.error ?: "Erro ao carregar ranking",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.refreshRanking() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                Text("Tentar Novamente")
                            }
                        }
                    }
                    // Lista de jogadores
                    else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 16.dp
                            )
                        ) {
                            // Widget de Top 3 jogadores (apenas quando filtrando por 'Todos' ou 'Top Notas')
                            if (uiState.filterState.currentFilter == com.guicarneirodev.ltascore.domain.models.RankingFilter.ALL ||
                                uiState.filterState.currentFilter == com.guicarneirodev.ltascore.domain.models.RankingFilter.TOP_RATED) {
                                item {
                                    // Pega os top 3 jogadores
                                    val topPlayers = uiState.filteredPlayers.take(3)
                                    if (topPlayers.isNotEmpty()) {
                                        TopPlayersWidget(
                                            topPlayers = topPlayers,
                                            title = "Top Jogadores",
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }

                            // Cabeçalho com contagem
                            item {
                                Text(
                                    text = "Mostrando ${uiState.filteredPlayers.size} jogadores",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LTAThemeColors.TextSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            // Lista de jogadores
                            itemsIndexed(
                                items = uiState.filteredPlayers,
                                key = { _, item -> item.player.id }
                            ) { index, item ->
                                PlayerRankingListItem(
                                    item = item,
                                    position = index + 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Função de pré-visualização do componente EmptyRankingView
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingScreenHeader(
    title: String,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isLoading: Boolean
) {
    TopAppBar(
        title = { Text(title) },
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
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        actions = {
            // Botão de atualizar
            IconButton(
                onClick = onRefreshClick,
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Atualizar"
                )
            }
        }
    )
}