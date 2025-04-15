package com.guicarneirodev.ltascore.android.ui.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.domain.models.VoteComment
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun CommentSection(
    voteId: String,
    comments: List<VoteComment>,
    currentUserId: String,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var commentText by remember { mutableStateOf("") }
    var showComments by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Cabeçalho dos comentários
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showComments = !showComments }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (comments.isEmpty()) "Comentários" else "Comentários (${comments.size})",
                color = LTAThemeColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = if (showComments) Icons.Default.Close else Icons.Default.Send,
                contentDescription = if (showComments) "Esconder comentários" else "Mostrar comentários",
                tint = LTAThemeColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        // Lista de comentários
        AnimatedVisibility(
            visible = showComments,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                if (comments.isNotEmpty()) {
                    comments.forEach { comment ->
                        CommentItem(
                            comment = comment,
                            isCurrentUser = comment.userId == currentUserId,
                            onDelete = { onDeleteComment(comment.id) }
                        )
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = LTAThemeColors.DarkBackground
                    )
                }

                // Campo para adicionar comentário
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Adicionar comentário...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = LTAThemeColors.CardBackground,
                            unfocusedContainerColor = LTAThemeColors.CardBackground,
                            focusedBorderColor = LTAThemeColors.PrimaryGold,
                            unfocusedBorderColor = LTAThemeColors.DarkBackground
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (commentText.isNotEmpty()) {
                                onAddComment(commentText)
                                commentText = ""
                            }
                        },
                        enabled = commentText.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar",
                            tint = if (commentText.isNotEmpty()) LTAThemeColors.PrimaryGold else LTAThemeColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: VoteComment,
    isCurrentUser: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar do usuário
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isCurrentUser) LTAThemeColors.PrimaryGold.copy(alpha = 0.2f)
                    else LTAThemeColors.CardBackground
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.username.take(1).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentUser) LTAThemeColors.PrimaryGold else LTAThemeColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Conteúdo do comentário
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.username,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentUser) LTAThemeColors.PrimaryGold else LTAThemeColors.TextPrimary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatTime(comment.timestamp),
                    fontSize = 10.sp,
                    color = LTAThemeColors.TextSecondary
                )

                if (isCurrentUser) {
                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = comment.text,
                fontSize = 14.sp,
                color = LTAThemeColors.TextPrimary
            )
        }
    }
}

private fun formatTime(timestamp: Instant): String {
    val now = Clock.System.now()
    val diff = now - timestamp

    return when {
        diff.inWholeSeconds < 60 -> "agora"
        diff.inWholeMinutes < 60 -> "${diff.inWholeMinutes}m"
        diff.inWholeHours < 24 -> "${diff.inWholeHours}h"
        diff.inWholeDays < 7 -> "${diff.inWholeDays}d"
        else -> {
            val date = timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date
            "${date.dayOfMonth}/${date.monthNumber}"
        }
    }
}