package com.gchat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

/**
 * Displays an image message bubble
 */
@Composable
fun ImageMessageBubble(
    imageUrl: String,
    caption: String?,
    isSentByCurrentUser: Boolean,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSentByCurrentUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(8.dp)
    ) {
        // Image
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = "Shared image",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onImageClick(imageUrl) },
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        )
        
        // Caption (optional)
        if (!caption.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSentByCurrentUser)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

