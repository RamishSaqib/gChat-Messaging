package com.gchat.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * WaveformView - Visualize audio waveform
 * 
 * Supports two modes:
 * 1. Animated mode - for recording (animates bars)
 * 2. Static mode - for playback (shows recorded waveform with progress)
 */
@Composable
fun WaveformView(
    waveformData: List<Float>,
    progress: Float = 0f,
    isAnimated: Boolean = false,
    color: Color = Color.White,
    progressColor: Color = Color.White.copy(alpha = 0.5f),
    modifier: Modifier = Modifier
) {
    // Animation for recording mode
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveform_progress"
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barCount = waveformData.size.coerceAtLeast(1)
        val barWidth = (canvasWidth / barCount) * 0.6f // 60% width for bars, 40% for spacing
        val spacing = (canvasWidth / barCount) * 0.4f
        
        waveformData.forEachIndexed { index, amplitude ->
            // Calculate bar position
            val x = index * (barWidth + spacing) + spacing / 2
            
            // Calculate bar height based on amplitude (0.0 to 1.0)
            val normalizedAmplitude = amplitude.coerceIn(0f, 1f)
            val minHeight = canvasHeight * 0.1f // Minimum 10% height
            val barHeight = (canvasHeight * 0.9f * normalizedAmplitude).coerceAtLeast(minHeight)
            
            // Center the bar vertically
            val y = (canvasHeight - barHeight) / 2
            
            // Apply animation if in animated mode
            val displayHeight = if (isAnimated) {
                val animationPhase = ((animatedProgress + (index.toFloat() / barCount)) % 1f)
                barHeight * (0.5f + 0.5f * kotlin.math.sin(animationPhase * 2 * Math.PI).toFloat())
            } else {
                barHeight
            }
            
            // Determine color based on progress
            val barColor = if (!isAnimated && index.toFloat() / barCount <= progress) {
                color
            } else if (!isAnimated) {
                progressColor
            } else {
                color
            }
            
            // Draw rounded rectangle bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, (canvasHeight - displayHeight) / 2),
                size = Size(barWidth, displayHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

/**
 * Static waveform for playback
 */
@Composable
fun StaticWaveformView(
    waveformData: List<Float>,
    progress: Float = 0f,
    color: Color = Color.White,
    progressColor: Color = Color.White.copy(alpha = 0.5f),
    modifier: Modifier = Modifier
) {
    WaveformView(
        waveformData = waveformData,
        progress = progress,
        isAnimated = false,
        color = color,
        progressColor = progressColor,
        modifier = modifier
    )
}

/**
 * Animated waveform for recording
 */
@Composable
fun AnimatedWaveformView(
    waveformData: List<Float>,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    WaveformView(
        waveformData = waveformData,
        progress = 0f,
        isAnimated = true,
        color = color,
        progressColor = color,
        modifier = modifier
    )
}

/**
 * Generate default waveform data for empty state
 */
fun generateDefaultWaveform(size: Int = 40): List<Float> {
    return List(size) { index ->
        // Generate a sine wave pattern
        (0.3f + 0.2f * kotlin.math.sin(index * 0.5).toFloat()).coerceIn(0.1f, 1f)
    }
}

