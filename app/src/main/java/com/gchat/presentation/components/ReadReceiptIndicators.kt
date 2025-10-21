package com.gchat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
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
import coil.compose.AsyncImage
import com.gchat.domain.model.User

/**
 * Read receipt checkmarks for 1-on-1 chats
 * 
 * @param isRead Whether the message has been read by the recipient
 * @param isSent Whether the message has been sent (vs sending/failed)
 */
@Composable
fun ReadReceiptCheckmarks(
    isRead: Boolean,
    isSent: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isRead) {
        Color(0xFF00A5FF) // Blue for read
    } else {
        Color.Gray // Gray for sent but not read
    }
    
    if (isSent) {
        Icon(
            imageVector = if (isRead) Icons.Default.DoneAll else Icons.Default.Check,
            contentDescription = if (isRead) "Read" else "Sent",
            tint = color,
            modifier = modifier.size(16.dp)
        )
    }
}

/**
 * Shows profile picture bubbles of users who have read the message (for group chats)
 * 
 * @param readByUsers List of users who have read the message
 * @param maxVisible Maximum number of avatars to show before showing "+X"
 */
@Composable
fun ReadByAvatars(
    readByUsers: List<User>,
    maxVisible: Int = 3,
    modifier: Modifier = Modifier
) {
    if (readByUsers.isEmpty()) return
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show up to maxVisible avatars
        readByUsers.take(maxVisible).forEach { user ->
            ReadByAvatar(user = user)
        }
        
        // Show "+X" if there are more users
        val remainingCount = readByUsers.size - maxVisible
        if (remainingCount > 0) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$remainingCount",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * Individual small avatar for read receipts
 */
@Composable
private fun ReadByAvatar(
    user: User,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (user.profilePictureUrl != null) {
            AsyncImage(
                model = user.profilePictureUrl,
                contentDescription = "${user.displayName}'s profile picture",
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
            )
        } else {
            // Show initials if no profile picture
            Text(
                text = user.displayName.take(1).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

