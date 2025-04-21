package com.guicarneirodev.ltascore.android.ui.friends.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guicarneirodev.ltascore.android.LTAThemeColors
import com.guicarneirodev.ltascore.domain.models.VoteReaction

@Composable
fun ReactionBar(
    voteId: String,
    reactions: List<VoteReaction>,
    userReaction: VoteReaction?,
    onReactionSelected: (String) -> Unit,
    onReactionRemoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showReactionSelector by remember { mutableStateOf(false) }

    println("ReactionBar para $voteId: ${reactions.size} reações, userReaction=${userReaction?.reaction ?: "nenhuma"}")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val reactionCounts = reactions.groupBy { it.reaction }
                    .mapValues { it.value.size }

                println("Contadores de reações: $reactionCounts")

                reactionCounts.forEach { (emoji, count) ->
                    ReactionCounter(
                        emoji = emoji,
                        count = count,
                        isSelected = userReaction?.reaction == emoji,
                        onClick = {
                            if (userReaction?.reaction == emoji) {
                                println("Removendo reação $emoji")
                                onReactionRemoved()
                            } else {
                                println("Alterando para reação $emoji")
                                onReactionSelected(emoji)
                            }
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Reagir:",
                color = LTAThemeColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable {
                        println("Alternando seletor de reações: ${!showReactionSelector}")
                        showReactionSelector = !showReactionSelector
                    }
                    .padding(end = 4.dp)
            )

            AnimatedVisibility(
                visible = showReactionSelector,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReactionEmoji("👍", userReaction?.reaction == "👍") {
                        println("Selecionada reação 👍")
                        onReactionSelected("👍")
                        showReactionSelector = false
                    }
                    ReactionEmoji("🔥", userReaction?.reaction == "🔥") {
                        println("Selecionada reação 🔥")
                        onReactionSelected("🔥")
                        showReactionSelector = false
                    }
                    ReactionEmoji("👎", userReaction?.reaction == "👎") {
                        println("Selecionada reação 👎")
                        onReactionSelected("👎")
                        showReactionSelector = false
                    }
                    ReactionEmoji("😮", userReaction?.reaction == "😮") {
                        println("Selecionada reação 😮")
                        onReactionSelected("😮")
                        showReactionSelector = false
                    }
                    ReactionEmoji("❤️", userReaction?.reaction == "❤️") {
                        println("Selecionada reação ❤️")
                        onReactionSelected("❤️")
                        showReactionSelector = false
                    }
                }
            }
        }
    }
}

@Composable
fun ReactionEmoji(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isSelected) LTAThemeColors.PrimaryGold.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Text(
            text = emoji,
            fontSize = 16.sp
        )
    }
}

@Composable
fun ReactionCounter(
    emoji: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) LTAThemeColors.PrimaryGold.copy(alpha = 0.2f)
                else LTAThemeColors.CardBackground
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 14.sp
            )

            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) LTAThemeColors.PrimaryGold else LTAThemeColors.TextPrimary
            )
        }
    }
}