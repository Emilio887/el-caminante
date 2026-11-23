package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = HighContrastGreen,
    onPrimary = Color.Black,
    primaryContainer = PineGreenDark,
    onPrimaryContainer = HighContrastGreen,
    secondary = TrailOrangeLight,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF4E2600),
    onSecondaryContainer = TrailOrangeLight,
    background = DarkBackground,
    onBackground = Color(0xFFE2E8E4),
    surface = DarkSurface,
    onSurface = Color(0xFFE2E8E4),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC0CCC4),
    error = OffTrackRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PineGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = PineGreenDark,
    secondary = TrailOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFFE65100),
    background = LightBackground,
    onBackground = Color(0xFF191C1A),
    surface = LightSurface,
    onSurface = Color(0xFF191C1A),
    surfaceVariant = LightCard,
    onSurfaceVariant = Color(0xFF404943),
    error = OffTrackRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent high-contrast outdoors palette
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
