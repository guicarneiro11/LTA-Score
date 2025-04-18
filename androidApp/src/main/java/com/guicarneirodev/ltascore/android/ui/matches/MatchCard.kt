package com.guicarneirodev.ltascore.android.ui.matches

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.MatchState

@Composable
fun MatchCard(
    match: Match,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = match.state == MatchState.COMPLETED, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = LTAThemeColors.CardBackground
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with week and time information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.blockName,
                    style = MaterialTheme.typography.labelMedium,
                    color = LTAThemeColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = formatDate(match.startTime.toString()),
                    style = MaterialTheme.typography.labelMedium,
                    color = LTAThemeColors.TextSecondary
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFF333340)
            )

            // Centered match score with consistent layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team 1 Container with fixed width and centered content
                Box(
                    modifier = Modifier
                        .width(120.dp)  // Fixed width to ensure consistency
                        .padding(end = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Logo
                        LogoImage(
                            imageUrl = match.teams[0].imageUrl,
                            name = match.teams[0].name,
                            code = match.teams[0].code
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Team code and score with fixed width
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.width(50.dp)  // Fixed width for team code and score
                        ) {
                            Text(
                                text = match.teams[0].code,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = LTAThemeColors.TextPrimary,
                                textAlign = TextAlign.End
                            )

                            Text(
                                text = match.teams[0].result.gameWins.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = LTAThemeColors.TextPrimary,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                // VS in the center
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "vs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LTAThemeColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                // Team 2 Container with fixed width and centered content
                Box(
                    modifier = Modifier
                        .width(120.dp)  // Fixed width to ensure consistency
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Team code and score with fixed width
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.width(50.dp)  // Fixed width for team code and score
                        ) {
                            Text(
                                text = match.teams[1].code,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = LTAThemeColors.TextPrimary,
                                textAlign = TextAlign.Start
                            )

                            Text(
                                text = match.teams[1].result.gameWins.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = LTAThemeColors.TextPrimary,
                                textAlign = TextAlign.Start
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Logo
                        LogoImage(
                            imageUrl = match.teams[1].imageUrl,
                            name = match.teams[1].name,
                            code = match.teams[1].code
                        )
                    }
                }
            }

            val (statusText, statusColor) = when (match.state) {
                MatchState.UNSTARTED -> Pair("Em breve", LTAThemeColors.Warning)
                MatchState.INPROGRESS -> Pair("AO VIVO", LTAThemeColors.LiveRed)
                MatchState.COMPLETED -> Pair("Concluída - Clique para votar", LTAThemeColors.Success)
            }

            if (match.state == MatchState.INPROGRESS) {
                Surface(
                    color = LTAThemeColors.LiveRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "live_indicator")
                        val animatedSize by infiniteTransition.animateFloat(
                            initialValue = 8f,
                            targetValue = 12f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_animation"
                        )

                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(animatedSize.dp)
                                .clip(CircleShape)
                                .background(LTAThemeColors.LiveRed)
                        )

                        Text(
                            text = statusText,
                            color = LTAThemeColors.LiveRed,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (match.state == MatchState.COMPLETED) FontWeight.Medium else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}