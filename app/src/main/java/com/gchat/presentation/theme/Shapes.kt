package com.gchat.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * iOS-inspired shapes with consistent corner radii
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Message bubble shapes
val MessageBubbleShape = RoundedCornerShape(20.dp)
val MessageBubbleShapeSent = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 20.dp,
    bottomEnd = 4.dp // Tail effect
)
val MessageBubbleShapeReceived = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 4.dp, // Tail effect
    bottomEnd = 20.dp
)

// For grouped messages (first in group)
val MessageBubbleShapeFirst = RoundedCornerShape(20.dp)

// For grouped messages (middle in group)
val MessageBubbleShapeMiddleSent = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 20.dp,
    bottomEnd = 4.dp
)
val MessageBubbleShapeMiddleReceived = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
    bottomStart = 4.dp,
    bottomEnd = 20.dp
)

// For grouped messages (last in group - same as regular with tail)
val MessageBubbleShapeLastSent = MessageBubbleShapeSent
val MessageBubbleShapeLastReceived = MessageBubbleShapeReceived

