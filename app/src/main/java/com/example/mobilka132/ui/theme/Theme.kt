package com.example.mobilka132.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    LIGHT, DARK
}

@Composable
fun Mobilka132Theme(
    themeMode: ThemeMode = if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.LIGHT,
    customColor: Color = TSUBlue,
    content: @Composable () -> Unit
) {
    val colorScheme = if (themeMode == ThemeMode.DARK) {
        darkColorScheme(
            primary = customColor,
            onPrimary = Color.White,
            primaryContainer = customColor.copy(alpha = 0.2f),
            onPrimaryContainer = customColor,
            secondary = Color.White,
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFF333333),
            onSecondaryContainer = Color.White,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF222222),
            onSurfaceVariant = customColor,
            error = Color(0xFFFFB4AB)
        )
    } else {
        lightColorScheme(
            primary = customColor,
            onPrimary = Color.White,
            primaryContainer = customColor.copy(alpha = 0.1f),
            onPrimaryContainer = customColor,
            secondary = Color.Black,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFF0F0F0),
            onSecondaryContainer = customColor,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color(0xFFF5F5F5),
            onSurfaceVariant = customColor,
            error = Color.Red,
            errorContainer = Color(0xFFFFDAD6),
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
