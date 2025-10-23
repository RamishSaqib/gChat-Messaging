package com.gchat.presentation.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gchat.domain.model.ReplyCategory
import com.gchat.domain.model.SmartReply

/**
 * Smart Reply Suggestions Component
 * 
 * Displays AI-generated reply suggestions as horizontally scrollable chips
 * above the message input field
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SmartReplySuggestions(
    replies: List<SmartReply>,
    isLoading: Boolean,
    error: String?,
    onReplySelected: (SmartReply) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-dismiss after 30 seconds
    LaunchedEffect(replies) {
        if (replies.isNotEmpty()) {
            kotlinx.coroutines.delay(30_000) // 30 seconds
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = isLoading || replies.isNotEmpty() || error != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // Header with dismiss button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Smart Replies",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss suggestions",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Content: Loading, Error, or Replies
                when {
                    isLoading -> {
                        LoadingShimmer()
                    }
                    error != null -> {
                        ErrorState(
                            error = error,
                            onRetry = onRetry
                        )
                    }
                    replies.isNotEmpty() -> {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(replies) { reply ->
                                SmartReplyChip(
                                    reply = reply,
                                    onClick = { onReplySelected(reply) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual smart reply chip
 */
@Composable
private fun SmartReplyChip(
    reply: SmartReply,
    onClick: () -> Unit
) {
    val containerColor = when (reply.category) {
        ReplyCategory.AFFIRMATIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        ReplyCategory.NEGATIVE -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
        ReplyCategory.QUESTION -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
        ReplyCategory.NEUTRAL -> MaterialTheme.colorScheme.secondaryContainer
    }
    
    val contentColor = when (reply.category) {
        ReplyCategory.AFFIRMATIVE -> MaterialTheme.colorScheme.primary
        ReplyCategory.NEGATIVE -> MaterialTheme.colorScheme.error
        ReplyCategory.QUESTION -> MaterialTheme.colorScheme.tertiary
        ReplyCategory.NEUTRAL -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 36.dp)
            .widthIn(max = 200.dp),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = reply.replyText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Loading shimmer effect
 */
@Composable
private fun LoadingShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(3) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                    )
            )
        }
    }
}

/**
 * Error state with retry button
 */
@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        
        IconButton(
            onClick = onRetry,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Retry",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

