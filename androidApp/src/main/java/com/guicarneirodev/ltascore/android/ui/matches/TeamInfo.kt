package com.guicarneirodev.ltascore.android.ui.matches

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun TeamInfo(
    name: String,
    code: String,
    imageUrl: String,
    wins: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(60.dp)
                .padding(4.dp)
        )
        Text(
            text = code,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = wins.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

internal fun formatDate(dateString: String): String {
    try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault()

        val date = inputFormat.parse(dateString) ?: return dateString
        return outputFormat.format(date)
    } catch (_: Exception) {
        return dateString
    }
}