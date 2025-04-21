package com.guicarneirodev.ltascore.android.ui.voting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

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

        Text(
            text = "$team1Score - $team2Score",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

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
