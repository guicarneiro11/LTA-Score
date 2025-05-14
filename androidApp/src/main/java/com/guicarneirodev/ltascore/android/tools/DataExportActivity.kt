package com.guicarneirodev.ltascore.android.tools

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.guicarneirodev.ltascore.data.datasource.static.PlayersDataSource
import com.guicarneirodev.ltascore.data.datasource.static.TeamLogoMapper
import com.guicarneirodev.ltascore.domain.models.Team
import com.guicarneirodev.ltascore.domain.models.TeamResult
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import com.guicarneirodev.ltascore.util.DataExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DataExportActivity : ComponentActivity() {
    private val matchRepository: MatchRepository by inject()
    private val playersDataSource = PlayersDataSource()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var exportStatus by remember { mutableStateOf("") }
            var isExporting by remember { mutableStateOf(false) }
            var exportedZipUri by remember { mutableStateOf<Uri?>(null) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Exportar Dados para xtool",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isExporting) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Exportando dados... $exportStatus")
                } else {
                    Button(
                        onClick = {
                            isExporting = true
                            exportStatus = "Iniciando..."

                            lifecycleScope.launch {
                                try {
                                    val zipUri = exportData { status ->
                                        exportStatus = status
                                    }
                                    exportedZipUri = zipUri
                                    isExporting = false
                                } catch (e: Exception) {
                                    exportStatus = "Erro: ${e.message}"
                                    isExporting = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Exportar Dados")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                exportedZipUri?.let { uri ->
                    Button(
                        onClick = {
                            shareFile(uri)
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Compartilhar Arquivo Exportado")
                    }
                }
            }
        }
    }

    private suspend fun exportData(updateStatus: (String) -> Unit): Uri {
        val dataDir = File(cacheDir, "exported-data").apply {
            deleteRecursively()
            mkdirs()
        }

        val leagues = listOf("lta_s", "lta_n", "cd")

        updateStatus("Exportando partidas...")
        leagues.forEach { league ->
            val matches = matchRepository.getMatches(league).first()
            val json = DataExporter.exportMatchesToJson(matches)
            File(dataDir, "matches_${league}.json").writeText(json)
            updateStatus("Exportadas ${matches.size} partidas para $league")
        }

        updateStatus("Exportando jogadores...")
        val players = playersDataSource.getAllPlayers()
        val playersJson = DataExporter.exportPlayersToJson(players)
        File(dataDir, "players.json").writeText(playersJson)

        updateStatus("Exportando times...")
        val teams = listOf(
            "pain-gaming", "loud", "isurus-estral", "leviatan", "furia",
            "keyd", "red", "fxw7", "team-liquid-honda", "flyquest",
            "shopify-rebellion", "dignitas", "cloud9-kia", "100-thieves",
            "disguised", "lyon", "red-kalunga-academy", "keyd-academy",
            "los", "flamengo", "ratz", "dopamina", "stellae", "rise",
            "kabum-idl", "corinthians"
        ).map { teamId ->
            Team(
                id = teamId,
                name = "",
                code = "",
                imageUrl = TeamLogoMapper.getTeamLogoUrl(teamId),
                players = emptyList(),
                result = TeamResult(null, 0, 0, 0)
            )
        }
        val teamsJson = DataExporter.exportTeamsToJson(teams)
        File(dataDir, "teams.json").writeText(teamsJson)

        updateStatus("Compactando arquivos...")
        val zipFile = File(cacheDir, "ltascore_data.zip")
        zipFile.delete()

        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            dataDir.listFiles()?.forEach { file ->
                zipOut.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }

        return FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            zipFile
        )
    }

    private fun shareFile(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar dados exportados"))
    }
}