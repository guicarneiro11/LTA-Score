package com.guicarneirodev.ltascore.android.ui.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.guicarneirodev.ltascore.android.LTAThemeColors
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun TeamInfo(
    name: String,
    code: String,
    imageUrl: String,
    wins: Int,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    if (alignment == Alignment.Start) {
        // Layout para times à esquerda
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            // Logo do time
            LogoImage(
                imageUrl = imageUrl,
                name = name,
                code = code
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Código e placar
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = LTAThemeColors.TextPrimary
                )

                Text(
                    text = wins.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = LTAThemeColors.TextPrimary
                )
            }
        }
    } else if (alignment == Alignment.End) {
        // Layout para times à direita
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            // Código e placar
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = LTAThemeColors.TextPrimary
                )

                Text(
                    text = wins.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = LTAThemeColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Logo do time
            LogoImage(
                imageUrl = imageUrl,
                name = name,
                code = code
            )
        }
    } else {
        // Layout centralizado (padrão)
        Column(
            modifier = modifier,
            horizontalAlignment = alignment
        ) {
            // Logo do time
            LogoImage(
                imageUrl = imageUrl,
                name = name,
                code = code
            )

            Spacer(modifier = Modifier.padding(vertical = 4.dp))

            // Código do time
            Text(
                text = code,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = LTAThemeColors.TextPrimary
            )

            // Número de vitórias
            Text(
                text = wins.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = LTAThemeColors.TextPrimary
            )
        }
    }
}

@Composable
fun LogoImage(
    imageUrl: String,
    name: String,
    code: String
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = name,
        contentScale = ContentScale.Fit,
        loading = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333340)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = code.take(1),
                    fontWeight = FontWeight.Bold,
                    color = LTAThemeColors.TextPrimary
                )
            }
        },
        error = {
            // Ícone de escudo em caso de erro
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333340)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Logo do time $name",
                    tint = LTAThemeColors.TextSecondary
                )
            }
        },
        modifier = Modifier.size(40.dp)
    )
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