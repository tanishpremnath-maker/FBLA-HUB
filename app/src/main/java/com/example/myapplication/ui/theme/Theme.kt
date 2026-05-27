package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AppGold,
    onPrimary = AppDarkText,
    primaryContainer = AppLightSecondary,
    onPrimaryContainer = AppDarkText,
    secondary = AppLightNavBar,
    onSecondary = AppDarkText,
    secondaryContainer = AppLightSecondary,
    onSecondaryContainer = AppDarkText,
    tertiary = AppGold,
    onTertiary = AppDarkText,
    background = AppLightBg,
    onBackground = AppDarkText,
    surface = AppLightSurface,
    onSurface = AppDarkText,
    surfaceVariant = AppLightSecondary,
    onSurfaceVariant = AppDarkText,
    outline = Color(0xFFB0BBD0),
    error = Color(0xFFDC143C),
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = AppGold,
    onPrimary = AppDarkText,
    primaryContainer = AppDarkSurface,
    onPrimaryContainer = Color.White,
    secondary = AppDarkSurface,
    onSecondary = Color.White,
    secondaryContainer = AppDarkSurface,
    onSecondaryContainer = Color.White,
    tertiary = AppGold,
    onTertiary = AppDarkText,
    background = AppDarkNavy,
    onBackground = Color.White,
    surface = AppDarkSurface,
    onSurface = Color.White,
    surfaceVariant = AppDarkSurface,
    onSurfaceVariant = Color.White,
    outline = Color(0xFF3A4A60),
    error = Color(0xFFFF6B6B),
    onError = Color.White,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
