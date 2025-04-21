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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.domain.models.PlayerRankingItem

@Composable
fun TopPlayersWidget(
    modifier: Modifier = Modifier,
    topPlayers: List<PlayerRankingItem>,
    title: String = "Melhores Avaliações",
) {
    if (topPlayers.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LTAThemeColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LTAThemeColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (topPlayers.isNotEmpty()) {
                    val firstPlayer = topPlayers[0]
                    TopPlayerItem(
                        player = firstPlayer,
                        position = 1,
                        modifier = Modifier.weight(1.2f),
                        size = 100.dp,
                        medalColor = Color(0xFFFFD700)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (topPlayers.size >= 2) {
                    val secondPlayer = topPlayers[1]
                    TopPlayerItem(
                        player = secondPlayer,
                        position = 2,
                        modifier = Modifier.weight(1f),
                        size = 80.dp,
                        medalColor = Color(0xFFC0C0C0)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (topPlayers.size >= 3) {
                    val thirdPlayer = topPlayers[2]
                    TopPlayerItem(
                        player = thirdPlayer,
                        position = 3,
                        modifier = Modifier.weight(1f),
                        size = 80.dp,
                        medalColor = Color(0xFFCD7F32)
                    )
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun TopPlayerItem(
    player: PlayerRankingItem,
    position: Int,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 100.dp,
    medalColor: Color
) {
    val ratingColor = when {
        player.averageRating < 3.0 -> Color(0xFFE57373)
        player.averageRating < 5.0 -> Color(0xFFFFB74D)
        player.averageRating < 7.0 -> Color(0xFFFFD54F)
        player.averageRating < 9.0 -> Color(0xFF81C784)
        else -> Color(0xFF4CAF50)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = medalColor,
            modifier = Modifier.size(24.dp)
        ) {
            Text(
                text = "$position",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(3.dp, medalColor, CircleShape)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(player.player.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = player.player.nickname,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.teamCode.take(1),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = player.player.nickname,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = LTAThemeColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = String.format("%.1f", player.averageRating),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ratingColor,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(40.dp)
                .height(3.dp)
                .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp * (player.averageRating.toFloat() / 10f))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(ratingColor.copy(alpha = 0.7f), ratingColor)
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}