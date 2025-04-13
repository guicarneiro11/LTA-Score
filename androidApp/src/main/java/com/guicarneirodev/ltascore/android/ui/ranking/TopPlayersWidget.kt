package com.guicarneirodev.ltascore.android.ui.ranking

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

/**
 * Widget que exibe os três jogadores com as melhores avaliações
 * Este componente pode ser usado na tela de ranking ou em outras partes do app
 */
@Composable
fun TopPlayersWidget(
    topPlayers: List<PlayerRankingItem>,
    title: String = "Melhores Avaliações",
    modifier: Modifier = Modifier
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
            // Título
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LTAThemeColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Top 3 jogadores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Player 1 (centro e maior)
                if (topPlayers.size >= 1) {
                    val firstPlayer = topPlayers[0]
                    TopPlayerItem(
                        player = firstPlayer,
                        position = 1,
                        modifier = Modifier.weight(1.2f),
                        size = 100.dp,
                        medalColor = Color(0xFFFFD700) // Ouro
                    )
                }

                // Espaço entre os jogadores
                Spacer(modifier = Modifier.width(8.dp))

                // Player 2 (esquerda)
                if (topPlayers.size >= 2) {
                    val secondPlayer = topPlayers[1]
                    TopPlayerItem(
                        player = secondPlayer,
                        position = 2,
                        modifier = Modifier.weight(1f),
                        size = 80.dp,
                        medalColor = Color(0xFFC0C0C0) // Prata
                    )
                }

                // Espaço entre os jogadores
                Spacer(modifier = Modifier.width(8.dp))

                // Player 3 (direita)
                if (topPlayers.size >= 3) {
                    val thirdPlayer = topPlayers[2]
                    TopPlayerItem(
                        player = thirdPlayer,
                        position = 3,
                        modifier = Modifier.weight(1f),
                        size = 80.dp,
                        medalColor = Color(0xFFCD7F32) // Bronze
                    )
                }
            }
        }
    }
}

/**
 * Item individual de um jogador no pódio do top 3
 */

@Composable
fun TopPlayerItem(
    player: PlayerRankingItem,
    position: Int,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 100.dp,
    medalColor: Color
) {
    val ratingColor = when {
        player.averageRating < 3.0 -> Color(0xFFE57373) // Vermelho claro
        player.averageRating < 5.0 -> Color(0xFFFFB74D) // Laranja claro
        player.averageRating < 7.0 -> Color(0xFFFFD54F) // Amarelo
        player.averageRating < 9.0 -> Color(0xFF81C784) // Verde claro
        else -> Color(0xFF4CAF50) // Verde
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Posição (1º, 2º ou 3º)
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

        // Foto do jogador
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

            // Badge do time no canto inferior direito
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

        // Nome do jogador
        Text(
            text = player.player.nickname,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = LTAThemeColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // Nota
        Text(
            text = String.format("%.1f", player.averageRating),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ratingColor,
            textAlign = TextAlign.Center
        )

        // Barra de rating visual
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