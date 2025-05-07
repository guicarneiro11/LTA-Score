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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.guicarneirodev.ltascore.android.BuildConfig
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.R
import com.guicarneirodev.ltascore.android.viewmodels.NotificationSettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState(initial = NotificationSettingsViewModel.NotificationSettingsUiState())
    val matchId by viewModel.matchId.collectAsState()
    val matchState by viewModel.matchState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LTAThemeColors.DarkBackground)
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.notification_settings)) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = LTAThemeColors.CardBackground,
                titleContentColor = LTAThemeColors.TextPrimary,
                navigationIconContentColor = LTAThemeColors.TextPrimary
            )
        )

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

        Spacer(modifier = Modifier.height(32.dp))

        if (BuildConfig.DEBUG) {
            Text(
                text = "Depuração",
                style = MaterialTheme.typography.titleLarge,
                color = LTAThemeColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.sendTestNotification() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LTAThemeColors.CardBackground,
                    contentColor = LTAThemeColors.PrimaryGold
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Testar Notificação")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Testar Mudança de Estado",
                style = MaterialTheme.typography.titleMedium,
                color = LTAThemeColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = matchId,
                onValueChange = { viewModel.updateMatchId(it) },
                label = { Text("ID da Partida") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            var expanded by remember { mutableStateOf(false) }
            val states = listOf("UNSTARTED", "INPROGRESS", "COMPLETED")

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = matchState,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    states.forEach { state ->
                        DropdownMenuItem(
                            text = { Text(state) },
                            onClick = {
                                viewModel.updateMatchState(state)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.forceUpdateMatchState() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LTAThemeColors.CardBackground,
                    contentColor = LTAThemeColors.SecondaryRed
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Forçar Mudança de Estado")
            }

            if (uiState.success != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.success!!,
                    color = LTAThemeColors.Success,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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