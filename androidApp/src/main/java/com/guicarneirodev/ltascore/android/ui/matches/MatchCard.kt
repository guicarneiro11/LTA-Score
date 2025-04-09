package com.guicarneirodev.ltascore.android.ui.matches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.MatchState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchCard(
    match: Match,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = match.state == MatchState.COMPLETED, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabeçalho com data e tipo de partida
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = match.blockName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatDate(match.startTime.toString()),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time 1
                TeamInfo(
                    name = match.teams[0].name,
                    code = match.teams[0].code,
                    imageUrl = match.teams[0].imageUrl,
                    wins = match.teams[0].result.gameWins,
                    modifier = Modifier.weight(1f)
                )

                // VS
                Text(
                    text = "vs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Time 2
                TeamInfo(
                    name = match.teams[1].name,
                    code = match.teams[1].code,
                    imageUrl = match.teams[1].imageUrl,
                    wins = match.teams[1].result.gameWins,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status da partida
            val statusText = when (match.state) {
                MatchState.UNSTARTED -> "Em breve"
                MatchState.INPROGRESS -> "Ao vivo"
                MatchState.COMPLETED -> "Concluída - Clique para votar"
            }

            val statusColor = when (match.state) {
                MatchState.UNSTARTED -> MaterialTheme.colorScheme.tertiary
                MatchState.INPROGRESS -> Color.Red
                MatchState.COMPLETED -> MaterialTheme.colorScheme.primary
            }

            Text(
                text = statusText,
                color = statusColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}