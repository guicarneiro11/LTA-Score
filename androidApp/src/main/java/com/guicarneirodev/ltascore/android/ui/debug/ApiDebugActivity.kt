package com.guicarneirodev.ltascore.android.ui.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.guicarneirodev.ltascore.api.LoLEsportsApi
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ApiDebugActivity : ComponentActivity() {

    private val api: LoLEsportsApi by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                ApiDebugScreen(
                    onTestApi = { leagueSlug ->
                        testApi(leagueSlug)
                    }
                )
            }
        }
    }

    private fun testApi(leagueSlug: String) {
        lifecycleScope.launch {
            try {
                api.getSchedule(leagueSlug)
            } catch (_: Exception) {

            }
        }
    }
}

@Composable
fun ApiDebugScreen(
    onTestApi: (String) -> Unit
) {
    val logText by remember { mutableStateOf("Logs de API aparecerão aqui...") }
    var selectedLeague by remember { mutableStateOf("lta_s") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "API Debug Tool",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Liga:")
            Spacer(modifier = Modifier.width(8.dp))

            DropdownMenu(
                expanded = false,
                onDismissRequest = { },
            ) {
                DropdownMenuItem(
                    text = { Text("LTA Sul") },
                    onClick = { selectedLeague = "lta_s" }
                )
                DropdownMenuItem(
                    text = { Text("LTA Norte") },
                    onClick = { selectedLeague = "lta_n" }
                )
            }

            Text(
                text = if (selectedLeague == "lta_s") "LTA Sul" else "LTA Norte",
                modifier = Modifier.padding(8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onTestApi(selectedLeague) }
            ) {
                Text("Testar API")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = logText,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}
