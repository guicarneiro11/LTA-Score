package com.guicarneirodev.ltascore.android.ui.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.ui.matches.LogoImage
import com.guicarneirodev.ltascore.android.viewmodels.EditProfileViewModel
import com.guicarneirodev.ltascore.android.viewmodels.TeamFilterItem
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // IMPORTANTE: Efeito para mostrar um Toast quando salvar com sucesso
    val context = LocalContext.current
    LaunchedEffect(key1 = uiState.success) {
        if (uiState.success != null) {
            // Mostrar Toast e navegar de volta após salvar com sucesso
            Toast.makeText(context, uiState.success, Toast.LENGTH_SHORT).show()

            // Aguardar 1 segundo antes de retornar à tela anterior
            delay(1000)
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LTAThemeColors.CardBackground,
                    titleContentColor = LTAThemeColors.TextPrimary,
                    navigationIconContentColor = LTAThemeColors.TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .background(LTAThemeColors.DarkBackground)
        ) {
            Text(
                text = "Escolha seu time favorito",
                style = MaterialTheme.typography.titleLarge,
                color = LTAThemeColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading && uiState.availableTeams.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LTAThemeColors.PrimaryGold)
                }
            } else {
                // Grid de times
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.availableTeams.size) { index ->
                        val team = uiState.availableTeams[index]
                        TeamCard(
                            team = team,
                            isSelected = team.id == uiState.selectedTeamId,
                            onClick = { viewModel.selectTeam(team.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botão de salvar
                Button(
                    onClick = { viewModel.saveProfile() },
                    enabled = !uiState.isLoading && uiState.selectedTeamId != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LTAThemeColors.PrimaryGold,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Salvar",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Mensagem de sucesso
                if (uiState.success != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.success ?: "",
                        color = Color.Green,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Mensagem de erro
                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.error ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun TeamCard(
    team: TeamFilterItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                LTAThemeColors.CardBackground.copy(alpha = 0.7f)
            else
                LTAThemeColors.CardBackground
        ),
        border = if (isSelected)
            BorderStroke(2.dp, LTAThemeColors.PrimaryGold)
        else
            null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp)
            ) {
                // Logo do time
                LogoImage(
                    imageUrl = team.imageUrl,
                    name = team.name,
                    code = team.code,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Nome do time
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = LTAThemeColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            // Ícone de verificação quando selecionado
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                        .background(LTAThemeColors.PrimaryGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selecionado",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}