package com.guicarneirodev.ltascore.android.ui.matches

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guicarneirodev.ltascore.android.viewmodels.MatchFilter
import com.guicarneirodev.ltascore.android.viewmodels.MatchesViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    viewModel: MatchesViewModel = koinViewModel(),
    onMatchClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMatches()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Cabeçalho com título
        TopAppBar(
            title = {
                Text(
                    text = "LTA Score",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Tabs para alternar entre ligas
        TabRow(
            selectedTabIndex = uiState.selectedLeagueIndex,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            uiState.availableLeagues.forEachIndexed { index, league ->
                Tab(
                    selected = index == uiState.selectedLeagueIndex,
                    onClick = { viewModel.selectLeague(index) },
                    text = { Text(league.name) }
                )
            }
        }

        // Subtítulo que indica claramente Split 2
        Text(
            text = "Split 2 - 2025",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Filtros de estado das partidas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.filter == MatchFilter.ALL,
                onClick = { viewModel.setFilter(MatchFilter.ALL) },
                label = { Text("Todas") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = uiState.filter == MatchFilter.UPCOMING,
                onClick = { viewModel.setFilter(MatchFilter.UPCOMING) },
                label = { Text("Próximas") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = uiState.filter == MatchFilter.COMPLETED,
                onClick = { viewModel.setFilter(MatchFilter.COMPLETED) },
                label = { Text("Concluídas") },
                modifier = Modifier.weight(1f)
            )
        }

        // Status da requisição e contagens
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
                        MatchFilter.ALL -> "Todas as partidas"
                        MatchFilter.UPCOMING -> "Próximas partidas"
                        MatchFilter.COMPLETED -> "Partidas concluídas"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!uiState.isLoading) {
                    Text(
                        text = "${uiState.filteredMatches.size} partidas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Linha separadora
        Divider(modifier = Modifier.padding(horizontal = 16.dp))

        // Lista de partidas ou estados de erro/carregamento
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Carregando partidas...")
                }
            } else if (uiState.error != null) {
                // Exibe o erro de forma mais visível
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Erro",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.error ?: "Erro desconhecido",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadMatches() }) {
                        Text("Tentar Novamente")
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
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when(uiState.filter) {
                            MatchFilter.ALL -> "Nenhuma partida encontrada no Split 2"
                            MatchFilter.UPCOMING -> "Não há próximas partidas agendadas"
                            MatchFilter.COMPLETED -> "Nenhuma partida concluída ainda"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Lista de partidas quando tudo estiver ok
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Agrupe as partidas por dia
                    val matchesByDay = uiState.filteredMatches.groupBy { match ->
                        // Formato: "DD/MM"
                        val dateTime = match.startTime.toLocalDateTime(TimeZone.currentSystemDefault())
                        "${dateTime.date.dayOfMonth.toString().padStart(2, '0')}/${dateTime.date.monthNumber.toString().padStart(2, '0')}"
                    }

                    matchesByDay.forEach { (day, matchesOnDay) ->
                        // Cabeçalho do dia
                        item {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // Partidas do dia
                        items(matchesOnDay) { match ->
                            MatchCard(
                                match = match,
                                onClick = { onMatchClick(match.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}