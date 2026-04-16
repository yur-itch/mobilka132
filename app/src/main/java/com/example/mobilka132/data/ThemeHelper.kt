package com.example.mobilka132.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.mobilka132.ui.theme.ThemeMode

object ThemeHelper {
    private const val PREFS_NAME = "settings"
    private const val THEME_KEY = "Theme.Helper.Selected.Theme"
    private const val COLOR_KEY = "Theme.Helper.Custom.Color"

    fun getTheme(context: Context): ThemeMode {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = preferences.getString(THEME_KEY, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
        return try {
            ThemeMode.valueOf(themeName)
        } catch (e: Exception) {
            ThemeMode.LIGHT
        }
    }

    fun setTheme(context: Context, theme: ThemeMode) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit().putString(THEME_KEY, theme.name).apply()
    }

    fun getCustomColor(context: Context): Color {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorArgb = preferences.getInt(COLOR_KEY, Color(0xFF0054A6).toArgb())
        return Color(colorArgb)
    }

    fun setCustomColor(context: Context, color: Color) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences.edit().putInt(COLOR_KEY, color.toArgb()).apply()
    }
}
