package com.lyrics.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GreenDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color(0xFF003909),
    primaryContainer = Color(0xFF00531A),
    onPrimaryContainer = Color(0xFF77FF7E),
    secondary = Color(0xFF52DB6B),
    onSecondary = Color(0xFF003914),
    secondaryContainer = Color(0xFF00531E),
    onSecondaryContainer = Color(0xFF73F884),
    background = Color(0xFF0A0F0A),
    surface = Color(0xFF0A0F0A),
    surfaceVariant = Color(0xFF1A2A1A),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFAABBAA),
)

private val GreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF1B6E24),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA8F5A8),
    onPrimaryContainer = Color(0xFF002106),
    secondary = Color(0xFF2E6B38),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB0F0B8),
    onSecondaryContainer = Color(0xFF00210C),
    background = Color(0xFFF5FBF5),
    surface = Color(0xFFF5FBF5),
    surfaceVariant = Color(0xFFDEEEDE),
)

@Composable
fun LyricsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
