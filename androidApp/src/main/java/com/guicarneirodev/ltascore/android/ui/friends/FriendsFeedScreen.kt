package com.guicarneirodev.ltascore.android.ui.friends

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.android.ui.friends.social.CommentSection
import com.guicarneirodev.ltascore.android.ui.friends.social.ReactionBar
import com.guicarneirodev.ltascore.android.viewmodels.FriendsFeedViewModel
import com.guicarneirodev.ltascore.android.viewmodels.VoteReactionsState
import com.guicarneirodev.ltascore.domain.models.FriendVoteHistoryItem
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import com.guicarneirodev.ltascore.domain.models.VoteComment
import com.guicarneirodev.ltascore.domain.models.VoteReaction
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsFeedScreen(
    viewModel: FriendsFeedViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feed de Amigos") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LTAThemeColors.PrimaryGold,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadFeed() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LTAThemeColors.DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = LTAThemeColors.PrimaryGold)
            } else if (uiState.error != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = uiState.error ?: "Erro desconhecido",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.loadFeed() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tentar Novamente")
                    }
                }
            } else if (uiState.feed.isEmpty()) {
                EmptyFeedContent()
            } else {
                FriendsFeedContent(
                    groupedFeed = uiState.groupedFeed,
                    voteReactions = uiState.voteReactions,
                    voteComments = uiState.voteComments,
                    currentUserId = uiState.currentUserId,
                    onAddReaction = { voteId, reaction -> viewModel.addReaction(voteId, reaction) },
                    onRemoveReaction = { voteId -> viewModel.removeReaction(voteId) },
                    onAddComment = { voteId, text -> viewModel.addComment(voteId, text) },
                    onDeleteComment = { commentId -> viewModel.deleteComment(commentId) }
                )
            }
        }
    }
}

@Composable
fun EmptyFeedContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = LTAThemeColors.TextSecondary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Nenhuma atividade recente",
            style = MaterialTheme.typography.titleMedium,
            color = LTAThemeColors.TextPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Seus amigos ainda não fizeram avaliações ou você não adicionou amigos.",
            style = MaterialTheme.typography.bodyMedium,
            color = LTAThemeColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(
                containerColor = LTAThemeColors.PrimaryGold
            )
        ) {
            Text("Adicionar Amigos")
        }
    }
}

@Composable
fun FriendsFeedContent(
    groupedFeed: Map<String, List<FriendVoteHistoryItem>>,
    voteReactions: Map<String, VoteReactionsState>,
    voteComments: Map<String, List<VoteComment>>,
    currentUserId: String,
    onAddReaction: (String, String) -> Unit,
    onRemoveReaction: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Para cada grupo (amigo + partida)
        groupedFeed.forEach { (groupKey, votes) ->
            // Extrair informações do grupo
            val parts = groupKey.split(":")
            val friendName = parts[0].trim()
            val matchInfo = parts.getOrNull(1)?.trim()?.split("|") ?: listOf("", "")
            val dateStr = matchInfo.getOrNull(1) ?: ""

            // Cabeçalho do grupo
            item(key = "header_$groupKey") {
                FeedGroupHeader(
                    friendName = friendName,
                    date = dateStr,
                    teams = "${votes.firstOrNull()?.teamCode ?: ""} vs ${votes.firstOrNull()?.opponentTeamCode ?: ""}",
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Itens de voto deste grupo
            items(votes, key = { it.id }) { vote ->
                // Obter reações e comentários para este voto
                val reactionsState = voteReactions[vote.id] ?: VoteReactionsState()
                val commentsList = voteComments[vote.id] ?: emptyList()

                FriendVoteItem(
                    vote = vote,
                    reactions = reactionsState.reactions,
                    userReaction = reactionsState.userReaction,
                    comments = commentsList,
                    currentUserId = currentUserId,
                    onAddReaction = { reaction -> onAddReaction(vote.id, reaction) },
                    onRemoveReaction = { onRemoveReaction(vote.id) },
                    onAddComment = { text -> onAddComment(vote.id, text) },
                    onDeleteComment = onDeleteComment
                )
            }

            // Separador entre grupos
            item(key = "divider_$groupKey") {
                Divider(
                    color = Color(0xFF333340),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun FeedGroupHeader(
    friendName: String,
    date: String,
    teams: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Nome do amigo
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar do amigo (simplificado)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(LTAThemeColors.PrimaryGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friendName.take(1).uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = LTAThemeColors.PrimaryGold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = friendName,
                style = MaterialTheme.typography.titleMedium,
                color = LTAThemeColors.PrimaryGold,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Data da partida
        Text(
            text = date,
            style = MaterialTheme.typography.labelMedium,
            color = LTAThemeColors.TextSecondary
        )

        // Times da partida
        Text(
            text = teams,
            style = MaterialTheme.typography.titleMedium,
            color = LTAThemeColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun FriendVoteItem(
    vote: FriendVoteHistoryItem,
    reactions: List<VoteReaction>,
    userReaction: VoteReaction?,
    comments: List<VoteComment>,
    currentUserId: String,
    onAddReaction: (String) -> Unit,
    onRemoveReaction: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ratingColor = when {
        vote.rating < 3.0f -> Color(0xFFE57373) // Vermelho claro
        vote.rating < 5.0f -> Color(0xFFFFB74D) // Laranja claro
        vote.rating < 7.0f -> Color(0xFFFFD54F) // Amarelo
        vote.rating < 9.0f -> Color(0xFF81C784) // Verde claro
        else -> Color(0xFF4CAF50) // Verde
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LTAThemeColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Conteúdo do voto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Foto do jogador
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(vote.playerImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = vote.playerNickname,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                // Informações do jogador
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = vote.playerNickname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LTAThemeColors.TextPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Badge de posição
                        PositionBadge(position = vote.playerPosition)
                    }
                }

                // Nota média
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Nota média em destaque
                    Text(
                        text = String.format("%.1f", vote.rating),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ratingColor
                    )

                    // Barra de rating visual
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(60.dp * (vote.rating.toFloat() / 10f))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(ratingColor.copy(alpha = 0.7f), ratingColor)
                                    ),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }

            // Barra de reações
            ReactionBar(
                voteId = vote.id,  // Adicionando o ID do voto aqui
                reactions = reactions,
                userReaction = userReaction,
                onReactionSelected = onAddReaction,
                onReactionRemoved = onRemoveReaction
            )

            // Separador sutil
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 16.dp)
                .background(LTAThemeColors.DarkBackground)
            )

            // Seção de comentários - CORRIGIDO: Adicionado o parâmetro voteId
            CommentSection(
                voteId = vote.id,  // Adicionando o ID do voto aqui
                comments = comments,
                currentUserId = currentUserId,
                onAddComment = onAddComment,
                onDeleteComment = onDeleteComment
            )
        }
    }
}

@Composable
fun PositionBadge(position: PlayerPosition) {
    val (backgroundColor, textColor) = when(position) {
        PlayerPosition.TOP -> Color(0xFF3498db) to Color.White
        PlayerPosition.JUNGLE -> Color(0xFF2ecc71) to Color.White
        PlayerPosition.MID -> Color(0xFFe74c3c) to Color.White
        PlayerPosition.ADC -> Color(0xFFf39c12) to Color.White
        PlayerPosition.SUPPORT -> Color(0xFF9b59b6) to Color.White
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor,
        modifier = Modifier.height(20.dp)
    ) {
        Text(
            text = position.name,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}