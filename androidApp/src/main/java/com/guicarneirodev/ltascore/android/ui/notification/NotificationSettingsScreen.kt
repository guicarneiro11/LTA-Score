package com.guicarneirodev.ltascore.android.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.guicarneirodev.ltascore.android.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.viewmodels.NotificationSettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LTAThemeColors.DarkBackground)
            .padding(16.dp)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text(stringResource(R.string.notification_settings)) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = LTAThemeColors.PrimaryGold,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        // Content
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.notification_preferences),
            style = MaterialTheme.typography.titleLarge,
            color = LTAThemeColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        NotificationSettingItem(
            title = stringResource(R.string.match_start_notifications),
            description = stringResource(R.string.match_start_notifications_desc),
            checked = uiState.matchNotifications,
            onCheckedChange = { viewModel.updateMatchNotifications(it) }
        )

        NotificationSettingItem(
            title = stringResource(R.string.live_match_notifications),
            description = stringResource(R.string.live_match_notifications_desc),
            checked = uiState.liveMatchNotifications,
            onCheckedChange = { viewModel.updateLiveMatchNotifications(it) }
        )

        NotificationSettingItem(
            title = stringResource(R.string.result_notifications),
            description = stringResource(R.string.result_notifications_desc),
            checked = uiState.resultNotifications,
            onCheckedChange = { viewModel.updateResultNotifications(it) }
        )

        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = LTAThemeColors.PrimaryGold
            )
        }

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = LTAThemeColors.SecondaryRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun NotificationSettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = LTAThemeColors.TextPrimary
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = LTAThemeColors.TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LTAThemeColors.PrimaryGold,
                checkedTrackColor = LTAThemeColors.PrimaryGold.copy(alpha = 0.5f),
                uncheckedThumbColor = LTAThemeColors.TextSecondary,
                uncheckedTrackColor = LTAThemeColors.CardBackground
            )
        )
    }
}