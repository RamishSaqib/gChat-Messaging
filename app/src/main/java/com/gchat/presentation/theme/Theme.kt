package com.gchat.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * gChat Material 3 Theme
 * 
 * Inspired by iMessage's clean, modern design
 */

// Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0B6EFD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3D),
    
    secondary = Color(0xFF34C759),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F5DD),
    onSecondaryContainer = Color(0xFF002109),
    
    tertiary = Color(0xFFFF9500),
    onTertiary = Color.White,
    
    error = Color(0xFFFF3B30),
    onError = Color.White,
    
    background = Color.White,
    onBackground = Color.Black,
    
    surface = Color(0xFFF5F5F5),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF8E8E93),
    
    outline = Color(0xFFE5E5EA),
    outlineVariant = Color(0xFFC7C7CC)
)

// Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0B6EFD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD6E3FF),
    
    secondary = Color(0xFF34C759),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF005313),
    onSecondaryContainer = Color(0xFFD6F5DD),
    
    tertiary = Color(0xFFFF9500),
    onTertiary = Color.Black,
    
    error = Color(0xFFFF3B30),
    onError = Color.White,
    
    background = Color.Black,
    onBackground = Color.White,
    
    surface = Color(0xFF1C1C1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    
    outline = Color(0xFF38383A),
    outlineVariant = Color(0xFF48484A)
)

@Composable
fun GChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

