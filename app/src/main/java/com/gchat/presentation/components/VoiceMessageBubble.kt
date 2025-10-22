package com.gchat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gchat.domain.repository.PlaybackState

/**
 * VoiceMessageBubble - Display voice message with playback controls
 * 
 * Features:
 * - Play/pause button
 * - Waveform visualization with progress
 * - Duration / current time display
 * - Playback speed control (0.5x, 1x, 1.5x, 2x)
 * - Expandable transcription
 */
@Composable
fun VoiceMessageBubble(
    audioDuration: Int,
    audioWaveform: List<Float>?,
    transcription: String?,
    isTranscribing: Boolean,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    currentSpeed: Float,
    isOwnMessage: Boolean,
    modifier: Modifier = Modifier
) {
    var showTranscription by remember { mutableStateOf(false) }
    
    val waveform = audioWaveform ?: generateDefaultWaveform(40)
    
    Column(
        modifier = modifier
            .widthIn(min = 280.dp, max = 320.dp)
    ) {
        // Main playback UI
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Play/Pause button
            PlayPauseButton(
                playbackState = playbackState,
                onClick = onPlayPause,
                isOwnMessage = isOwnMessage
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Waveform and info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Time and speed controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time display
                    Text(
                        text = formatTime(playbackState, audioDuration),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = if (isOwnMessage) {
                            Color.White.copy(alpha = 0.9f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    // Speed control
                    SpeedControl(
                        currentSpeed = currentSpeed,
                        onSpeedChange = onSpeedChange,
                        isOwnMessage = isOwnMessage
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Waveform
                val progress = when (playbackState) {
                    is PlaybackState.Playing -> playbackState.progress
                    is PlaybackState.Paused -> playbackState.progress
                    else -> 0f
                }
                
                StaticWaveformView(
                    waveformData = waveform,
                    progress = progress,
                    color = if (isOwnMessage) Color.White else MaterialTheme.colorScheme.primary,
                    progressColor = if (isOwnMessage) {
                        Color.White.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                )
            }
        }
        
        // Transcription section
        if (isTranscribing || transcription != null) {
            Divider(
                color = if (isOwnMessage) {
                    Color.White.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                thickness = 1.dp
            )
            
            TranscriptionSection(
                transcription = transcription,
                isTranscribing = isTranscribing,
                isExpanded = showTranscription,
                onToggle = { showTranscription = !showTranscription },
                isOwnMessage = isOwnMessage
            )
        }
    }
}

@Composable
private fun PlayPauseButton(
    playbackState: PlaybackState,
    onClick: () -> Unit,
    isOwnMessage: Boolean
) {
    val isPlaying = playbackState is PlaybackState.Playing
    
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isOwnMessage) {
                    Color.White.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            )
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = if (isOwnMessage) Color.White else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun SpeedControl(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    isOwnMessage: Boolean
) {
    val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
    val currentIndex = speeds.indexOf(currentSpeed)
    
    TextButton(
        onClick = {
            val nextIndex = (currentIndex + 1) % speeds.size
            onSpeedChange(speeds[nextIndex])
        },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(28.dp)
    ) {
        Text(
            text = "${currentSpeed}x",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isOwnMessage) Color.White else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TranscriptionSection(
    transcription: String?,
    isTranscribing: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isOwnMessage: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isTranscribing) "Transcribing..." else "Transcription",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isOwnMessage) {
                    Color.White.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            if (isTranscribing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = if (isOwnMessage) Color.White else MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = if (isOwnMessage) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        AnimatedVisibility(
            visible = isExpanded && transcription != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = transcription ?: "",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = if (isOwnMessage) {
                    Color.White.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                },
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Format time display based on playback state
 */
private fun formatTime(playbackState: PlaybackState, totalDuration: Int): String {
    val currentSeconds = when (playbackState) {
        is PlaybackState.Playing -> playbackState.currentPosition
        is PlaybackState.Paused -> playbackState.currentPosition
        else -> 0
    }
    
    val current = formatDuration(currentSeconds)
    val total = formatDuration(totalDuration)
    
    return "$current / $total"
}

/**
 * Format duration in MM:SS format
 */
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}

