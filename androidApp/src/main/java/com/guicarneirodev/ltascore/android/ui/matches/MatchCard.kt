package com.guicarneirodev.ltascore.android.ui.matches

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.R
import com.guicarneirodev.ltascore.domain.models.Match
import com.guicarneirodev.ltascore.domain.models.MatchPredictionStats
import com.guicarneirodev.ltascore.domain.models.MatchState

@Composable
fun MatchCard(
    match: Match,
    onClick: () -> Unit,
    onWatchVodClick: ((String) -> Unit)? = null,
    predictionStats: MatchPredictionStats? = null,
    userPrediction: String? = null,
    isLoadingPrediction: Boolean = false,
    onPredictTeam: (String) -> Unit = {},
    weekTitle: String = match.blockName,
    isAdmin: Boolean = false,
    onAdminClick: ((String) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isAdmin || match.state == MatchState.COMPLETED, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = LTAThemeColors.CardBackground
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weekTitle,
                    style = MaterialTheme.typography.bodyMedium,
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .padding(end = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            LogoImage(
                                imageUrl = match.teams[0].imageUrl,
                                name = match.teams[0].name,
                                code = match.teams[0].code
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.width(50.dp)
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

                        if (match.state == MatchState.UNSTARTED) {
                            Spacer(modifier = Modifier.height(4.dp))

                            val team0Id = match.teams[0].id
                            val team0Percent = predictionStats?.percentages?.get(team0Id) ?: 0
                            val isTeam0Predicted = userPrediction == team0Id

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .height(24.dp)
                                    .background(
                                        if (isTeam0Predicted) LTAThemeColors.PrimaryGold.copy(alpha = 0.2f)
                                        else LTAThemeColors.CardBackground.copy(alpha = 0.8f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable(enabled = !isLoadingPrediction) { onPredictTeam(team0Id) }
                                    .border(
                                        width = 1.dp,
                                        color = if (isTeam0Predicted) LTAThemeColors.PrimaryGold.copy(alpha = 0.5f)
                                        else LTAThemeColors.TextSecondary.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingPrediction && userPrediction == null) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = LTAThemeColors.TextSecondary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.votes_percentage_format, team0Percent),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isTeam0Predicted) LTAThemeColors.PrimaryGold
                                        else LTAThemeColors.TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.vs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LTAThemeColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.width(50.dp)
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

                            LogoImage(
                                imageUrl = match.teams[1].imageUrl,
                                name = match.teams[1].name,
                                code = match.teams[1].code
                            )
                        }

                        if (match.state == MatchState.UNSTARTED) {
                            Spacer(modifier = Modifier.height(4.dp))

                            val team1Id = match.teams[1].id
                            val team1Percent = predictionStats?.percentages?.get(team1Id) ?: 0
                            val isTeam1Predicted = userPrediction == team1Id

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .height(24.dp)
                                    .background(
                                        if (isTeam1Predicted) LTAThemeColors.PrimaryGold.copy(alpha = 0.2f)
                                        else LTAThemeColors.CardBackground.copy(alpha = 0.8f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable(enabled = !isLoadingPrediction) { onPredictTeam(team1Id) }
                                    .border(
                                        width = 1.dp,
                                        color = if (isTeam1Predicted) LTAThemeColors.PrimaryGold.copy(alpha = 0.5f)
                                        else LTAThemeColors.TextSecondary.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingPrediction && userPrediction == null) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = LTAThemeColors.TextSecondary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.votes_percentage_format, team1Percent),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isTeam1Predicted) LTAThemeColors.PrimaryGold
                                        else LTAThemeColors.TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val (statusText, statusColor) = when (match.state) {
                MatchState.UNSTARTED -> Pair(stringResource(R.string.soon), LTAThemeColors.Warning)
                MatchState.INPROGRESS -> Pair(stringResource(R.string.live_now), LTAThemeColors.LiveRed)
                MatchState.COMPLETED -> Pair(stringResource(R.string.completed_click), LTAThemeColors.Success)
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

            if (isAdmin && match.state != MatchState.COMPLETED) {
                Surface(
                    color = LTAThemeColors.PrimaryGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { onAdminClick?.invoke(match.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = LTAThemeColors.PrimaryGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clique para gerenciar jogadores",
                            style = MaterialTheme.typography.bodySmall,
                            color = LTAThemeColors.PrimaryGold,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (match.state == MatchState.COMPLETED && onWatchVodClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onWatchVodClick(match.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LTAThemeColors.TertiaryGold
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.watch_vod),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}