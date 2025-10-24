package com.gchat.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gchat.domain.model.Message

/**
 * Displays reactions below a message bubble
 * 
 * Shows compact counts like Facebook Messenger: "👍 3  ❤️ 2"
 * Current user's reaction is highlighted
 */
@Composable
fun ReactionsDisplay(
    message: Message,
    currentUserId: String,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reactionCounts = message.getReactionCounts()
    val userReaction = message.getUserReaction(currentUserId)
    
    if (reactionCounts.isEmpty()) return
    
    Row(
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        reactionCounts.forEach { (emoji, count) ->
            ReactionChip(
                emoji = emoji,
                count = count,
                isUserReaction = emoji == userReaction,
                onClick = { onReactionClick(emoji) }
            )
        }
    }
}

@Composable
private fun ReactionChip(
    emoji: String,
    count: Int,
    isUserReaction: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isUserReaction) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    
    val borderColor = if (isUserReaction) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    
    val textColor = if (isUserReaction) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isUserReaction) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 14.sp
        )
        
        if (count > 1) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = if (isUserReaction) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

