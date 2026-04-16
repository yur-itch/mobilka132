package com.example.mobilka132.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    LIGHT, DARK, CUSTOM
}

private val DarkColorScheme = darkColorScheme(
    primary = TSUBlue,
    onPrimary = TSUWhite,
    primaryContainer = TSUBlue,
    onPrimaryContainer = TSUWhite,
    secondary = TSUWhite,
    onSecondary = TSUBlack,
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = TSUWhite,
    background = TSUBlack,
    onBackground = TSUWhite,
    surface = TSUBlack,
    onSurface = TSUWhite,
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = TSUWhite,
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = TSUBlue,
    onPrimary = TSUWhite,
    primaryContainer = TSUBlue,
    onPrimaryContainer = TSUWhite,
    secondary = TSUBlack,
    onSecondary = TSUWhite,
    secondaryContainer = Color(0xFFF0F0F0),
    onSecondaryContainer = TSUBlue,
    tertiary = TSUBlue,
    onTertiary = TSUWhite,
    background = TSUWhite,
    onBackground = TSUBlack,
    surface = TSUWhite,
    onSurface = TSUBlack,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = TSUBlue,
    error = Color.Red,
    errorContainer = Color(0xFFFFDAD6),
    onError = TSUWhite
)

@Composable
fun Mobilka132Theme(
    themeMode: ThemeMode = if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.LIGHT,
    customColor: Color = TSUBlue,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.CUSTOM -> {
            lightColorScheme(
                primary = customColor,
                onPrimary = Color.White,
                primaryContainer = customColor.copy(alpha = 0.1f),
                onPrimaryContainer = customColor,
                secondary = Color.Black,
                onSecondary = Color.White,
                background = Color.White,
                onBackground = Color.Black,
                surface = Color.White,
                onSurface = Color.Black,
                surfaceVariant = Color(0xFFF5F5F5),
                onSurfaceVariant = customColor
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
