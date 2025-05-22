package com.guicarneirodev.ltascore.android.ui.ranking

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.R
import com.guicarneirodev.ltascore.domain.models.PlayerRankingItem

@Composable
fun TopPlayersWidget(
    modifier: Modifier = Modifier,
    topPlayers: List<PlayerRankingItem>,
    title: String = stringResource(R.string.best_ratings),
) {
    if (topPlayers.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LTAThemeColors.CardBackground
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = LTAThemeColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                if (topPlayers.size >= 2) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        TopPlayerCard(
                            player = topPlayers[1],
                            position = 2,
                            size = 90.dp,
                            medalColor = Color(0xFFC0C0C0),
                            borderColor = Color(0xFF9E9E9E)
                        )
                    }
                }

                if (topPlayers.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).offset(y = (-16).dp)
                    ) {
                        TopPlayerCard(
                            player = topPlayers[0],
                            position = 1,
                            size = 110.dp,
                            medalColor = Color(0xFFFFD700),
                            borderColor = Color(0xFFDAA520),
                            isFirst = true
                        )
                    }
                }

                if (topPlayers.size >= 3) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        TopPlayerCard(
                            player = topPlayers[2],
                            position = 3,
                            size = 90.dp,
                            medalColor = Color(0xFFCD7F32),
                            borderColor = Color(0xFFA0522D)
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun TopPlayerCard(
    player: PlayerRankingItem,
    position: Int,
    size: androidx.compose.ui.unit.Dp,
    medalColor: Color,
    borderColor: Color,
    isFirst: Boolean = false
) {
    val ratingColor = when {
        player.averageRating >= 9.0 -> Color(0xFF4CAF50)
        player.averageRating >= 7.0 -> Color(0xFF81C784)
        player.averageRating >= 5.0 -> Color(0xFFFFD54F)
        player.averageRating >= 3.0 -> Color(0xFFFFB74D)
        else -> Color(0xFFE57373)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = medalColor,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .shadow(
                    elevation = if (isFirst) 8.dp else 4.dp,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Text(
                text = "$position°",
                fontSize = if (isFirst) 18.sp else 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        Box(contentAlignment = Alignment.Center) {
            if (isFirst) {
                Box(
                    modifier = Modifier
                        .size(size + 8.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    medalColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(player.player.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = player.player.nickname,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(
                        width = if (isFirst) 4.dp else 3.dp,
                        color = borderColor,
                        shape = CircleShape
                    )
            )

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(28.dp)
                    .border(2.dp, Color.White, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = player.teamCode.take(2).uppercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = player.player.nickname,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = LTAThemeColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF2A2A30),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = String.format("%.1f", player.averageRating),
                    fontSize = if (isFirst) 22.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ratingColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = ratingColor,
                    modifier = Modifier.size(if (isFirst) 18.dp else 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(if (isFirst) 80.dp else 70.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF1A1A1F))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(player.averageRating.toFloat() / 10f)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                ratingColor.copy(alpha = 0.7f),
                                ratingColor
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.votes_count, player.totalVotes),
            style = MaterialTheme.typography.labelMedium,
            color = LTAThemeColors.TextSecondary.copy(alpha = 0.8f)
        )
    }
}