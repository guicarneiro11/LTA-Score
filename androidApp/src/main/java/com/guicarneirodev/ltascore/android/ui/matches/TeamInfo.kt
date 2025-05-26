package com.guicarneirodev.ltascore.android.ui.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.data.datasource.static.TeamLogoMapper
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun LogoImage(
    name: String,
    code: String
) {
    val teamId = when(code) {
        "LOUD" -> "loud"
        "PAIN" -> "pain-gaming"
        "IE" -> "isurus-estral"
        "LEV" -> "leviatan"
        "FUR" -> "furia"
        "VKS" -> "keyd"
        "RED" -> "red"
        "FXW7" -> "fxw7"
        "C9" -> "cloud9-kia"
        "100T" -> "100-thieves"
        "FLY" -> "flyquest"
        "TL" -> "team-liquid-honda"
        "SR" -> "shopify-rebellion"
        "DIG" -> "dignitas"
        "LYON" -> "lyon"
        "DSG" -> "disguised"
        "LOS" -> "los"
        "FLA" -> "flamengo"
        "RATZ" -> "ratz"
        "DPM" -> "dopamina"
        "STE" -> "stellae"
        "RISE" -> "rise"
        "KBM" -> "kabum-idl"
        "SCCP" -> "corinthians"
        else -> null
    }

    val staticLogoUrl = TeamLogoMapper.getTeamLogoUrl(teamId)

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(staticLogoUrl)
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