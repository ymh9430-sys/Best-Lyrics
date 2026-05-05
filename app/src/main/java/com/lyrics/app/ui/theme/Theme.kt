package com.lyrics.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colors from reference design
private val SpotifyGreen = Color(0xFF1DB954)
private val DarkBackground = Color(0xFF0A0A0A)
private val DarkSurface = Color(0xFF111111)
private val DarkCard = Color(0xFF181818)
private val DarkElevated = Color(0xFF282828)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB3B3B3)

private val DarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0D2B1A),
    onPrimaryContainer = SpotifyGreen,
    secondary = SpotifyGreen,
    onSecondary = Color.Black,
    secondaryContainer = DarkElevated,
    onSecondaryContainer = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF535353),
    outlineVariant = Color(0xFF282828),
    error = Color(0xFFE91429),
    onError = Color.White,
    errorContainer = Color(0xFF3D0B0F),
    onErrorContainer = Color(0xFFFFB3B8),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1AA34A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F5E0),
    onPrimaryContainer = Color(0xFF002110),
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF606060),
)

@Composable
fun LyricsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
