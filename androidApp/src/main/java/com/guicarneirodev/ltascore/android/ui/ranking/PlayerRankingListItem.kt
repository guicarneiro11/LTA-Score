package com.guicarneirodev.ltascore.android.ui.ranking

import androidx.compose.foundation.background
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
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.PlayerRankingItem

/**
 * Item da lista de ranking que exibe as informações de um jogador
 */
@Composable
fun PlayerRankingListItem(
    item: PlayerRankingItem,
    position: Int, // Posição no ranking (1º, 2º, etc.)
    modifier: Modifier = Modifier
) {
    val ratingColor = when {
        item.averageRating < 3.0 -> Color(0xFFE57373) // Vermelho claro
        item.averageRating < 5.0 -> Color(0xFFFFB74D) // Laranja claro
        item.averageRating < 7.0 -> Color(0xFFFFD54F) // Amarelo
        item.averageRating < 9.0 -> Color(0xFF81C784) // Verde claro
        else -> Color(0xFF4CAF50) // Verde
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = LTAThemeColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Posição no ranking
            Text(
                text = "$position",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when (position) {
                    1 -> Color(0xFFFFD700) // Ouro
                    2 -> Color(0xFFC0C0C0) // Prata
                    3 -> Color(0xFFCD7F32) // Bronze
                    else -> LTAThemeColors.TextSecondary
                },
                modifier = Modifier.width(30.dp)
            )

            // Foto do jogador
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.player.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.player.nickname,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            // Informações do jogador
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = item.player.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Badge de posição
                    PositionBadge(position = item.position)

                    Spacer(modifier = Modifier.width(8.dp))

                    // Nome do time
                    Text(
                        text = item.teamCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = LTAThemeColors.TertiaryGold,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Total de votos
                    Text(
                        text = "${item.totalVotes} votos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Nota média
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Nota média em destaque
                Text(
                    text = String.format("%.1f", item.averageRating),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ratingColor
                )

                // Barra de rating visual
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(4.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(60.dp * (item.averageRating.toFloat() / 10f))
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
    }
}

@Composable
fun PositionBadge(position: PlayerPosition) {
    val (backgroundColor, textColor) = when(position) {
        PlayerPosition.TOP -> Color(0xFF3498db) to Color.White
        PlayerPosition.JUNGLE -> Color(0xFF2ecc71) to Color.White
        PlayerPosition.MID -> Color(0xFFe74c3c) to Color.White
        PlayerPosition.ADC -> Color(0xFFf39c12) to Color.White
        PlayerPosition.SUPPORT -> Color(0xFF9b59b6) to Color.White
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor,
        modifier = Modifier.height(20.dp)
    ) {
        Text(
            text = position.name,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}