package com.guicarneirodev.ltascore.android.ui.voting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.guicarneirodev.ltascore.domain.models.Player
import com.guicarneirodev.ltascore.domain.models.PlayerPosition

@Composable
fun PlayerVotingItem(
    player: Player,
    currentRating: Any, // Agora usando Float
    onRatingChanged: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Cabeçalho com info do jogador
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Foto do jogador com tratamento de erro
                AsyncImage(
                    model = player.imageUrl,
                    contentDescription = player.nickname,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Informações do jogador
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = player.nickname,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge da posição com cor
                        val positionColor = when(player.position) {
                            PlayerPosition.TOP -> Color(0xFF3498db)
                            PlayerPosition.JUNGLE -> Color(0xFF2ecc71)
                            PlayerPosition.MID -> Color(0xFFe74c3c)
                            PlayerPosition.ADC -> Color(0xFFf39c12)
                            PlayerPosition.SUPPORT -> Color(0xFF9b59b6)
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = positionColor,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = player.position.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Nome completo do jogador
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Utilizando o novo componente de avaliação decimal
            DecimalRatingBar(
                rating = currentRating as Float,
                onRatingChanged = onRatingChanged
            )
        }
    }
}