package com.guicarneirodev.ltascore.android.ui.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.viewmodels.TeamFilterItem
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.RankingFilter

/**
 * Componente que exibe as opções de filtro para o ranking
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RankingFilters(
    currentFilter: RankingFilter,
    onFilterSelected: (RankingFilter) -> Unit,
    onTeamSelected: (String?) -> Unit,
    onPositionSelected: (PlayerPosition?) -> Unit,
    selectedTeamId: String?,
    selectedPosition: PlayerPosition?,
    availableTeams: List<TeamFilterItem>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LTAThemeColors.DarkBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Campo de pesquisa
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Pesquisar jogador ou time...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Chips de filtro principal
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterOption(
                text = "Todos",
                icon = Icons.Default.Groups,
                isSelected = currentFilter == RankingFilter.ALL,
                onClick = { onFilterSelected(RankingFilter.ALL) }
            )

            FilterOption(
                text = "Top Notas",
                icon = Icons.Default.Star,
                isSelected = currentFilter == RankingFilter.TOP_RATED,
                onClick = { onFilterSelected(RankingFilter.TOP_RATED) }
            )

            FilterOption(
                text = "Mais Votados",
                icon = Icons.Default.ThumbUp,
                isSelected = currentFilter == RankingFilter.MOST_VOTED,
                onClick = { onFilterSelected(RankingFilter.MOST_VOTED) }
            )

            FilterOption(
                text = "Por Time",
                icon = Icons.Default.FilterAlt,
                isSelected = currentFilter == RankingFilter.BY_TEAM,
                onClick = { onFilterSelected(RankingFilter.BY_TEAM) }
            )

            FilterOption(
                text = "Por Posição",
                icon = Icons.Default.Person,
                isSelected = currentFilter == RankingFilter.BY_POSITION,
                onClick = { onFilterSelected(RankingFilter.BY_POSITION) }
            )

            FilterOption(
                text = "Semana",
                icon = Icons.Default.DateRange,
                isSelected = currentFilter == RankingFilter.BY_WEEK,
                onClick = { onFilterSelected(RankingFilter.BY_WEEK) }
            )

            FilterOption(
                text = "Mês",
                icon = Icons.Default.CalendarMonth,
                isSelected = currentFilter == RankingFilter.BY_MONTH,
                onClick = { onFilterSelected(RankingFilter.BY_MONTH) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filtros secundários condicionais
        when (currentFilter) {
            RankingFilter.BY_TEAM -> {
                TeamSelector(
                    teams = availableTeams,
                    selectedTeamId = selectedTeamId,
                    onTeamSelected = onTeamSelected
                )
            }
            RankingFilter.BY_POSITION -> {
                PositionSelector(
                    selectedPosition = selectedPosition,
                    onPositionSelected = onPositionSelected
                )
            }
            else -> {
                // Nenhum filtro secundário para outras opções
            }
        }

        Divider(
            modifier = Modifier.padding(top = 12.dp),
            color = LTAThemeColors.CardBackground
        )
    }
}

@Composable
fun FilterOption(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = LTAThemeColors.CardBackground,
            labelColor = LTAThemeColors.TextSecondary,
            iconColor = LTAThemeColors.TextSecondary,
            selectedContainerColor = LTAThemeColors.PrimaryGold,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = LTAThemeColors.CardBackground,
            selectedBorderColor = LTAThemeColors.PrimaryGold
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamSelector(
    teams: List<TeamFilterItem>,
    selectedTeamId: String?,
    onTeamSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Selecione um time:",
            style = MaterialTheme.typography.bodyMedium,
            color = LTAThemeColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = teams.find { it.id == selectedTeamId }?.name ?: "Selecione um time",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Opção para limpar o filtro
                DropdownMenuItem(
                    text = { Text("Todos os times") },
                    onClick = {
                        onTeamSelected(null)
                        expanded = false
                    }
                )

                // Lista de times disponíveis
                teams.forEach { team ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(team.code)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(team.name)
                            }
                        },
                        onClick = {
                            onTeamSelected(team.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PositionSelector(
    selectedPosition: PlayerPosition?,
    onPositionSelected: (PlayerPosition?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Selecione uma posição:",
            style = MaterialTheme.typography.bodyMedium,
            color = LTAThemeColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Opção para mostrar todas as posições
            AssistChip(
                onClick = { onPositionSelected(null) },
                label = { Text("Todas") },
                leadingIcon = null,
                border = null,  // Removendo border personalizado
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selectedPosition == null)
                        LTAThemeColors.PrimaryGold
                    else
                        LTAThemeColors.CardBackground,
                    labelColor = if (selectedPosition == null)
                        Color.White
                    else
                        LTAThemeColors.TextPrimary
                )
            )

            // Chips para cada posição
            PlayerPosition.entries.forEach { position ->
                val isSelected = selectedPosition == position
                val (backgroundColor, textColor) = when(position) {
                    PlayerPosition.TOP -> Color(0xFF3498db) to Color.White
                    PlayerPosition.JUNGLE -> Color(0xFF2ecc71) to Color.White
                    PlayerPosition.MID -> Color(0xFFe74c3c) to Color.White
                    PlayerPosition.ADC -> Color(0xFFf39c12) to Color.White
                    PlayerPosition.SUPPORT -> Color(0xFF9b59b6) to Color.White
                }

                AssistChip(
                    onClick = { onPositionSelected(position) },
                    label = { Text(position.name) },
                    leadingIcon = null,
                    border = null,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) backgroundColor else LTAThemeColors.CardBackground,
                        labelColor = if (isSelected) textColor else LTAThemeColors.TextPrimary
                    )
                )
            }
        }
    }
}