package com.guicarneirodev.ltascore.android.ui.voting

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@SuppressLint("DefaultLocale")
@Composable
fun DecimalRatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    maxRating: Float = 10f,
    steps: Int = 100
) {

    val formattedRating = String.format("%.1f", rating)
    val ratingColor = when {
        rating < 3f -> MaterialTheme.colorScheme.error
        rating < 6f -> MaterialTheme.colorScheme.tertiary
        rating < 8f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ratingColor)
        ) {
            Text(
                text = formattedRating,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Slider(
                value = rating,
                onValueChange = {
                    val roundedValue = (it * 10).roundToInt() / 10f
                    onRatingChanged(roundedValue)
                },
                valueRange = 0f..maxRating,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = ratingColor,
                    activeTrackColor = ratingColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${maxRating/2}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$maxRating",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}