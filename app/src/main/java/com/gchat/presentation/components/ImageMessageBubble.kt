package com.gchat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.gchat.domain.model.Message
import com.gchat.presentation.theme.MessageBubbleShapeReceived
import com.gchat.presentation.theme.MessageBubbleShapeSent

/**
 * Displays an image message bubble with iOS-style design
 */
@Composable
fun ImageMessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val imageUrl = message.mediaUrl ?: return
    val caption = message.text
    
    Surface(
        modifier = modifier.widthIn(max = 300.dp),
        shape = if (isCurrentUser) MessageBubbleShapeSent else MessageBubbleShapeReceived,
        shadowElevation = 0.5.dp
    ) {
        Box(
            modifier = Modifier.clip(if (isCurrentUser) MessageBubbleShapeSent else MessageBubbleShapeReceived)
        ) {
            // Image
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = "Shared image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp)
                    .clickable { onImageClick(imageUrl) },
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            )
            
            // Caption overlay at bottom (if present)
            if (!caption.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

