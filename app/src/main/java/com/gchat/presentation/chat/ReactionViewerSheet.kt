package com.gchat.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gchat.domain.model.Message
import com.gchat.domain.model.Reaction
import com.gchat.domain.model.User
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modal bottom sheet showing who reacted with what
 * 
 * Has tabs for each emoji type (All, 👍, ❤️, etc.)
 * Shows user avatars, names, and reaction timestamps
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionViewerSheet(
    message: Message,
    participantUsers: Map<String, User>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Build list of reactions with user info
    val reactions = remember(message.reactions, participantUsers) {
        buildReactionsList(message.reactions, participantUsers)
    }
    
    // Get unique emojis
    val emojis = remember(reactions) {
        listOf("All") + reactions.map { it.emoji }.distinct()
    }
    
    var selectedTab by remember { mutableStateOf(0) }
    
    // Filter reactions based on selected tab
    val filteredReactions = remember(selectedTab, reactions) {
        if (selectedTab == 0) {
            reactions
        } else {
            val selectedEmoji = emojis[selectedTab]
            reactions.filter { it.emoji == selectedEmoji }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Header
            Text(
                text = "Reactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                emojis.forEachIndexed { index, emoji ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (emoji == "All") {
                                    Text("All")
                                } else {
                                    Text(emoji, fontSize = 20.sp)
                                }
                                
                                val count = if (emoji == "All") {
                                    reactions.size
                                } else {
                                    reactions.count { it.emoji == emoji }
                                }
                                
                                Text(
                                    text = count.toString(),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Reactions list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredReactions, key = { "${it.userId}-${it.emoji}" }) { reaction ->
                    ReactionItem(reaction = reaction)
                }
            }
        }
    }
}

@Composable
private fun ReactionItem(
    reaction: Reaction,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User avatar
        if (reaction.user?.profilePictureUrl != null) {
            AsyncImage(
                model = reaction.user.profilePictureUrl,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = reaction.user?.displayName?.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        
        // User name
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = reaction.user?.displayName ?: "Unknown User",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = formatTimestamp(reaction.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Emoji
        Text(
            text = reaction.emoji,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Build a list of Reaction objects with user info from the reactions map
 */
private fun buildReactionsList(
    reactionsMap: Map<String, List<String>>,
    participantUsers: Map<String, User>
): List<Reaction> {
    return reactionsMap.flatMap { (emoji, userIds) ->
        userIds.map { userId ->
            Reaction(
                emoji = emoji,
                userId = userId,
                timestamp = System.currentTimeMillis(), // Note: actual timestamp not stored, using current time
                user = participantUsers[userId]
            )
        }
    }.sortedByDescending { it.timestamp }
}

/**
 * Format timestamp for display
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * Extended Reaction data class with user info for display
 */
private data class Reaction(
    val emoji: String,
    val userId: String,
    val timestamp: Long,
    val user: User? = null
)

