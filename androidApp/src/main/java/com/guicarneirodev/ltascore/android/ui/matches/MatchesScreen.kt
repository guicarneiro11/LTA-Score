package com.guicarneirodev.ltascore.android.ui.matches

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import org.koin.androidx.compose.koinViewModel

@Composable
fun MatchesScreen(
    viewModel: MatchesViewModel = koinViewModel(),
    onMatchClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        // Apenas para garantir que será carregado
        viewModel.loadMatches()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tabs para alternar entre ligas
        TabRow(selectedTabIndex = uiState.selectedLeagueIndex) {
            uiState.availableLeagues.forEachIndexed { index, league ->
                Tab(
                    selected = index == uiState.selectedLeagueIndex,
                    onClick = { viewModel.selectLeague(index) },
                    text = { Text(league.name) }
                )
            }
        }

        // Botão de recarga manual para testes
        Button(
            onClick = { viewModel.loadMatches() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text("Recarregar Partidas")
        }

        // Filtros de estado das partidas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = uiState.filter == MatchFilter.ALL,
                onClick = { viewModel.setFilter(MatchFilter.ALL) },
                label = { Text("Todas") }
            )
            FilterChip(
                selected = uiState.filter == MatchFilter.UPCOMING,
                onClick = { viewModel.setFilter(MatchFilter.UPCOMING) },
                label = { Text("Próximas") }
            )
            FilterChip(
                selected = uiState.filter == MatchFilter.COMPLETED,
                onClick = { viewModel.setFilter(MatchFilter.COMPLETED) },
                label = { Text("Concluídas") }
            )
        }

        // Status da requisição e contagens
        if (uiState.matches.isNotEmpty()) {
            Text(
                text = "Total de partidas: ${uiState.matches.size} (Filtradas: ${uiState.filteredMatches.size})",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

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
                Text(
                    text = "Nenhuma partida encontrada para os filtros selecionados",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // Lista de partidas quando tudo estiver ok
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.filteredMatches) { match ->
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