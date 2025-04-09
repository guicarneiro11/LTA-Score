package com.guicarneirodev.ltascore.android.ui.voting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun RatingCard(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val ratingColor = when {
        rating < 3f -> MaterialTheme.colorScheme.error
        rating < 6f -> MaterialTheme.colorScheme.tertiary
        rating < 8f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Exibição da nota atual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%.1f", rating),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = ratingColor,
                fontSize = 36.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Controles de ajuste
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Botões de decremento
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start
            ) {
                SmallAdjustButton(
                    text = "-1.0",
                    onClick = { onRatingChanged(maxOf(0f, rating - 1f)) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                SmallAdjustButton(
                    text = "-0.1",
                    onClick = { onRatingChanged(maxOf(0f, rating - 0.1f)) }
                )
            }

            // Botões de incremento
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End
            ) {
                SmallAdjustButton(
                    text = "+0.1",
                    onClick = { onRatingChanged(minOf(10f, rating + 0.1f)) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                SmallAdjustButton(
                    text = "+1.0",
                    onClick = { onRatingChanged(minOf(10f, rating + 1f)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Slider para ajuste contínuo
        Slider(
            value = rating,
            onValueChange = {
                // Arredonda para uma casa decimal
                val roundedValue = (it * 10).roundToInt() / 10f
                onRatingChanged(roundedValue)
            },
            valueRange = 0f..10f,
            steps = 100,
            colors = SliderDefaults.colors(
                thumbColor = ratingColor,
                activeTrackColor = ratingColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SmallAdjustButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}