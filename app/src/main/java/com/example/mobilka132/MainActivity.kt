package com.example.mobilka132

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.lifecycle.lifecycleScope
import com.example.mobilka132.data.location.LocationManager
import com.example.mobilka132.ui.theme.Mobilka132Theme
import com.example.mobilka132.data.LocaleHelper
import com.example.mobilka132.data.ThemeHelper
import com.example.mobilka132.ui.screens.MapScreen
import com.example.mobilka132.ui.theme.ThemeMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var mapManager: MapManager
    private val viewModel: MapViewModel by viewModels<MapViewModel>()
    private val location: LocationManager by lazy { 
        LocationManager(this, activityResultRegistry) { loc ->
            viewModel.updateLocation(loc)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mapManager = MapManager(this)

        lifecycleScope.launch {
            mapManager.loadData().await()
            viewModel.init(mapManager)
            viewModel.loadPointsFromAssets(this@MainActivity)

            viewModel.userWorldLocation?.let {
                viewModel.updateLocation(it)
            }
        }

        setContent {
            var currentTheme by remember { mutableStateOf(ThemeHelper.getTheme(this)) }
            var customColor by remember { mutableStateOf(ThemeHelper.getCustomColor(this)) }

            Mobilka132Theme(themeMode = currentTheme, customColor = customColor) {
                MapScreen(viewModel = viewModel, location = location, onLanguageChange = { lang ->
                    LocaleHelper.setLocale(this, lang)
                    recreate()
                }, onThemeChange = { theme, color ->
                    if (theme != null) {
                        ThemeHelper.setTheme(this, theme)
                        currentTheme = theme
                    }
                    if (color != null) {
                        ThemeHelper.setCustomColor(this, color)
                        customColor = color
                    }
                })
            }
        }
    }
}
