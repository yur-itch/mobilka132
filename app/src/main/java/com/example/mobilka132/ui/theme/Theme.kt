package com.example.mobilka132.ui.theme

import android.app.Activity
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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