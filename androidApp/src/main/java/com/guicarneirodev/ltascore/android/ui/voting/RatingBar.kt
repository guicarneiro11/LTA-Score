package com.guicarneirodev.ltascore.android.ui.voting

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun RatingBar(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    maxRating: Int = 10
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mostrar valor numérico
        Text(
            text = rating.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp)
        )

        // Slider para selecionar valor
        Slider(
            value = rating.toFloat(),
            onValueChange = { onRatingChanged(it.toInt()) },
            valueRange = 0f..maxRating.toFloat(),
            steps = maxRating - 1,
            modifier = Modifier.width(150.dp)
        )
    }
}