package com.guicarneirodev.ltascore.android.ui.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.viewmodels.RankingViewModel
import com.guicarneirodev.ltascore.android.viewmodels.TeamFilterItem
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.RankingFilter
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.MenuDefaults
import androidx.compose.ui.text.style.TextAlign
import com.guicarneirodev.ltascore.android.viewmodels.RankingUiState

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
    var showFilterMenu by remember { mutableStateOf(false) }

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
                    // Botão de filtro
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtros"
                        )
                    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LTAThemeColors.DarkBackground)
        ) {
            // Barra de pesquisa com design compacto
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Indicador do filtro ativo
            ActiveFilterIndicator(
                currentFilter = uiState.filterState.currentFilter,
                selectedTeamId = uiState.filterState.selectedTeamId,
                selectedPosition = uiState.filterState.selectedPosition,
                availableTeams = uiState.availableTeams,
                onClearFilter = { viewModel.setFilterType(RankingFilter.ALL) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Mostra seletores de sub-filtros quando necessário
            if (uiState.filterState.currentFilter == RankingFilter.BY_POSITION) {
                PositionFilterRow(
                    selectedPosition = uiState.filterState.selectedPosition,
                    onPositionSelected = viewModel::selectPosition,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            } else if (uiState.filterState.currentFilter == RankingFilter.BY_TEAM) {
                TeamFilterDropdown(
                    teams = uiState.availableTeams,
                    selectedTeamId = uiState.filterState.selectedTeamId,
                    onTeamSelected = viewModel::selectTeam,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Conteúdo principal: widget de top jogadores e lista
            RankingContent(
                uiState = uiState,
                viewModel = viewModel
            )
        }

        // Menu de filtros como diálogo (exibido quando clicar no botão de filtro)
        if (showFilterMenu) {
            FilterMenuDialog(
                currentFilter = uiState.filterState.currentFilter,
                onFilterSelected = { filter ->
                    viewModel.setFilterType(filter)
                    showFilterMenu = false
                },
                onDismiss = { showFilterMenu = false }
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Pesquisar jogador ou time...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = LTAThemeColors.CardBackground,
            focusedBorderColor = LTAThemeColors.PrimaryGold,
            unfocusedContainerColor = Color(0xFF1E1E24),
            focusedContainerColor = Color(0xFF1E1E24)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    )
}

@Composable
fun ActiveFilterIndicator(
    currentFilter: RankingFilter,
    selectedTeamId: String?,
    selectedPosition: PlayerPosition?,
    availableTeams: List<TeamFilterItem>,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentFilter != RankingFilter.ALL) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(LTAThemeColors.PrimaryGold.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (currentFilter) {
                        RankingFilter.TOP_RATED -> Icons.Default.Star
                        RankingFilter.MOST_VOTED -> Icons.Default.ThumbUp
                        RankingFilter.BY_TEAM -> Icons.Default.Group
                        RankingFilter.BY_POSITION -> Icons.Default.Person
                        else -> Icons.Default.FilterList
                    },
                    contentDescription = null,
                    tint = LTAThemeColors.PrimaryGold,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = when (currentFilter) {
                        RankingFilter.TOP_RATED -> "Top Notas"
                        RankingFilter.MOST_VOTED -> "Mais Votados"
                        RankingFilter.BY_TEAM -> {
                            val team = availableTeams.find { it.id == selectedTeamId }
                            "Time: ${team?.code ?: "Todos"}"
                        }
                        RankingFilter.BY_POSITION -> "Posição: ${selectedPosition?.name ?: "Todas"}"
                        else -> "Filtro Ativo"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = LTAThemeColors.PrimaryGold
                )
            }

            IconButton(
                onClick = onClearFilter,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Limpar filtro",
                    tint = LTAThemeColors.PrimaryGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun PositionFilterRow(
    selectedPosition: PlayerPosition?,
    onPositionSelected: (PlayerPosition?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Botão para todas as posições
        PositionFilterChip(
            selected = selectedPosition == null,
            position = null,
            onClick = { onPositionSelected(null) }
        )

        // Botões para cada posição
        PlayerPosition.entries.forEach { position ->
            PositionFilterChip(
                selected = selectedPosition == position,
                position = position,
                onClick = { onPositionSelected(position) }
            )
        }
    }
}

@Composable
fun PositionFilterChip(
    selected: Boolean,
    position: PlayerPosition?,
    onClick: () -> Unit
) {
    val backgroundColor = if (position == null) {
        if (selected) LTAThemeColors.PrimaryGold else LTAThemeColors.CardBackground
    } else {
        when(position) {
            PlayerPosition.TOP -> if (selected) Color(0xFF3498db) else Color(0xFF3498db).copy(alpha = 0.3f)
            PlayerPosition.JUNGLE -> if (selected) Color(0xFF2ecc71) else Color(0xFF2ecc71).copy(alpha = 0.3f)
            PlayerPosition.MID -> if (selected) Color(0xFFe74c3c) else Color(0xFFe74c3c).copy(alpha = 0.3f)
            PlayerPosition.ADC -> if (selected) Color(0xFFf39c12) else Color(0xFFf39c12).copy(alpha = 0.3f)
            PlayerPosition.SUPPORT -> if (selected) Color(0xFF9b59b6) else Color(0xFF9b59b6).copy(alpha = 0.3f)
        }
    }

    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(
            text = position?.name ?: "Todas",
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun FilterMenuDialog(
    currentFilter: RankingFilter,
    onFilterSelected: (RankingFilter) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LTAThemeColors.CardBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Filtrar Por",
                    style = MaterialTheme.typography.titleLarge,
                    color = LTAThemeColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Filtros principais
                FilterMenuItem(
                    icon = Icons.Default.FilterNone,
                    text = "Todos",
                    selected = currentFilter == RankingFilter.ALL,
                    onClick = { onFilterSelected(RankingFilter.ALL) }
                )

                FilterMenuItem(
                    icon = Icons.Default.Star,
                    text = "Top Notas",
                    selected = currentFilter == RankingFilter.TOP_RATED,
                    onClick = { onFilterSelected(RankingFilter.TOP_RATED) }
                )

                FilterMenuItem(
                    icon = Icons.Default.ThumbUp,
                    text = "Mais Votados",
                    selected = currentFilter == RankingFilter.MOST_VOTED,
                    onClick = { onFilterSelected(RankingFilter.MOST_VOTED) }
                )

                FilterMenuItem(
                    icon = Icons.Default.Group,
                    text = "Por Time",
                    selected = currentFilter == RankingFilter.BY_TEAM,
                    onClick = { onFilterSelected(RankingFilter.BY_TEAM) }
                )

                FilterMenuItem(
                    icon = Icons.Default.Person,
                    text = "Por Posição",
                    selected = currentFilter == RankingFilter.BY_POSITION,
                    onClick = { onFilterSelected(RankingFilter.BY_POSITION) }
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LTAThemeColors.PrimaryGold
                    )
                ) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Composable
fun FilterMenuItem(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) LTAThemeColors.PrimaryGold.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) LTAThemeColors.PrimaryGold else LTAThemeColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) LTAThemeColors.PrimaryGold else LTAThemeColors.TextPrimary
            )

            Spacer(modifier = Modifier.weight(1f))

            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = LTAThemeColors.PrimaryGold,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamFilterDropdown(
    teams: List<TeamFilterItem>,
    selectedTeamId: String?,
    onTeamSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = teams.find { it.id == selectedTeamId }?.name ?: "Selecione um time",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = LTAThemeColors.CardBackground,
                focusedBorderColor = LTAThemeColors.PrimaryGold,
                unfocusedContainerColor = Color(0xFF1E1E24),
                focusedContainerColor = Color(0xFF1E1E24)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(LTAThemeColors.CardBackground)
        ) {
            // Opção "Todos os Times"
            DropdownMenuItem(
                text = { Text("Todos os Times") },
                onClick = {
                    onTeamSelected(null)
                    expanded = false
                },
                colors = MenuDefaults.itemColors(
                    textColor = LTAThemeColors.TextPrimary,
                    leadingIconColor = LTAThemeColors.PrimaryGold
                )
            )

            Divider(color = Color(0xFF333340))

            // Lista de times
            teams.forEach { team ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Logo do time (mini)
                            AsyncImage(
                                model = team.imageUrl.takeIf { it.isNotBlank() },
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = team.code,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = team.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LTAThemeColors.TextSecondary
                                )
                            }
                        }
                    },
                    onClick = {
                        onTeamSelected(team.id)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = LTAThemeColors.TextPrimary,
                        leadingIconColor = LTAThemeColors.PrimaryGold
                    )
                )
            }
        }
    }
}

@Composable
fun RankingContent(
    uiState: RankingUiState,
    viewModel: RankingViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LTAThemeColors.DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isLoading) {
            // Estado de carregamento
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                CircularProgressIndicator(
                    color = LTAThemeColors.PrimaryGold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Carregando ranking...",
                    color = LTAThemeColors.TextSecondary
                )
            }
        } else if (uiState.error != null) {
            // Estado de erro
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
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
                    text = uiState.error,
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
        } else if (uiState.filteredPlayers.isEmpty()) {
            // Estado vazio
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = "Sem resultados",
                    tint = LTAThemeColors.TextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Nenhum jogador encontrado",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = LTAThemeColors.TextSecondary
                )
            }
        } else {
            // Lista de jogadores
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp
                )
            ) {
                // Top 3 jogadores (apenas para alguns filtros)
                if (shouldShowTopPlayersWidget(uiState.filterState.currentFilter)) {
                    item {
                        val topPlayers = uiState.filteredPlayers.take(3)
                        if (topPlayers.isNotEmpty()) {
                            Text(
                                text = "Top Jogadores",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = LTAThemeColors.TextPrimary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                            )

                            TopPlayersWidget(
                                topPlayers = topPlayers,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Contador de jogadores
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

// Função auxiliar para determinar quando mostrar o widget de Top Jogadores
private fun shouldShowTopPlayersWidget(filter: RankingFilter): Boolean {
    return filter == RankingFilter.ALL ||
            filter == RankingFilter.TOP_RATED ||
            filter == RankingFilter.MOST_VOTED
}