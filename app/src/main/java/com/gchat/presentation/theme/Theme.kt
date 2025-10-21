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
 * iOS-inspired design with clean, modern aesthetics
 */

// Light Color Scheme - iOS inspired
private val LightColorScheme = lightColorScheme(
    // Primary - iOS Blue
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    
    // Secondary - iOS Green
    secondary = Color(0xFF34C759),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E9EB), // Received message bubble
    onSecondaryContainer = Color(0xFF000000),
    
    // Tertiary - iOS Orange
    tertiary = Color(0xFFFF9500),
    onTertiary = Color.White,
    
    // Error - iOS Red
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    // Background - iOS light background
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF000000),
    
    // Surface - Pure white
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF8E8E93),
    
    // Outlines - Subtle dividers
    outline = Color(0xFFD1D1D6),
    outlineVariant = Color(0xFFE5E5EA)
)

// Dark Color Scheme - iOS inspired with true black
private val DarkColorScheme = darkColorScheme(
    // Primary - iOS Blue (slightly brighter for dark mode)
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003D7A),
    onPrimaryContainer = Color(0xFFD1E4FF),
    
    // Secondary - iOS Green
    secondary = Color(0xFF32D74B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2C2C2E), // Received message bubble dark
    onSecondaryContainer = Color(0xFFFFFFFF),
    
    // Tertiary - iOS Orange
    tertiary = Color(0xFFFF9F0A),
    onTertiary = Color.Black,
    
    // Error - iOS Red
    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Background - True black (OLED friendly)
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    
    // Surface - Dark gray
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    
    // Outlines - Subtle dividers for dark mode
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
        shapes = Shapes,
        content = content
    )
}

