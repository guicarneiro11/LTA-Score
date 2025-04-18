package com.guicarneirodev.ltascore.android.ui.friends.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.viewmodels.FriendRequestsUiState
import com.guicarneirodev.ltascore.domain.models.FriendRequest

@Composable
fun FriendRequestsSection(
    uiState: FriendRequestsUiState,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.requests.isEmpty()) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LTAThemeColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Título da seção
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = LTAThemeColors.PrimaryGold,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Solicitações de Amizade (${uiState.requests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LTAThemeColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de solicitações
            uiState.requests.forEach { request ->
                FriendRequestItem(
                    request = request,
                    onAccept = { onAcceptRequest(request.id) },
                    onReject = { onRejectRequest(request.id) }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Mensagens de erro ou sucesso
            if (uiState.error != null) {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.success != null) {
                Text(
                    text = uiState.success,
                    color = LTAThemeColors.Success,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun FriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = LTAThemeColors.CardBackground,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar (placeholder circular)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(LTAThemeColors.PrimaryGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = request.senderUsername.take(1).uppercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = LTAThemeColors.PrimaryGold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nome de usuário e mensagem
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = request.senderUsername,
                    style = MaterialTheme.typography.bodyLarge,
                    color = LTAThemeColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Quer ser seu amigo",
                    style = MaterialTheme.typography.bodySmall,
                    color = LTAThemeColors.TextSecondary
                )
            }

            // Botões de aceitar/recusar
            Row {
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LTAThemeColors.Success.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Aceitar",
                        tint = LTAThemeColors.Success
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onReject,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Recusar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}