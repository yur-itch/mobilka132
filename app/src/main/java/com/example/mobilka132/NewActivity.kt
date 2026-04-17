package com.example.mobilka132

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.mobilka132.data.ant.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewActivity : ComponentActivity() {

    private lateinit var mapManager: MapManager
    private var simulationState by mutableStateOf(SimulationFrame())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapManager = MapManager(this)

        setContent {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                SimulationCanvas(simulationState.ants, simulationState.spaces)
                DebugOverlay(simulationState.info)
            }
        }

        lifecycleScope.launch {
            mapManager.loadData().await()

            val simulation = CampusSimulation(
                width = mapManager.width,
                height = mapManager.height,
                grid = mapManager.grid,
                studentCount = 100 // Увеличим для наглядности
            )

            val startTime = System.currentTimeMillis()

            while (true) {
                // Выполняем расчеты в фоновом потоке
                simulation.update()

                // Обновляем UI-состояние
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                simulationState = SimulationFrame(
                    ants = simulation.ants.toList(), // Копируем список для потокобезопасности
                    spaces = simulation.spaces.toList(),
                    info = "Time: ${elapsed}s | Ants: ${simulation.ants.size} | " +
                            "Found: ${simulation.ants.count { it.hasFoundSpace }}"
                )

                delay(5) // ~60 FPS
            }
        }
    }
}

@Composable
fun SimulationCanvas(ants: List<Ant>, spaces: List<CoworkingSpace>) {
    val coworkings = remember(spaces) { spaces }
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Масштабируем 1500x1500 под размер экрана
        val scaleX = size.width / 750f
        val scaleY = size.height / 750f
        val minScale = minOf(scaleX, scaleY)

        coworkings.forEach { space ->
            drawCircle(
                color = if (space.currentStudents < space.capacity) Color.Green else Color.Red,
                radius = 12f * minScale,
                center = Offset(space.position.x.toFloat() * scaleX, space.position.y.toFloat() * scaleY)
            )
        }

        ants.forEach { ant ->
            drawCircle(
                color = if (ant.hasFoundSpace) Color.Yellow else Color.Cyan,
                radius = 6f * minScale,
                center = Offset(ant.x.toFloat() * scaleX, ant.y.toFloat() * scaleY)
            )
        }
    }
}

@Composable
fun DebugOverlay(info: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(8.dp)
    ) {
        Text(text = info, color = Color.White, fontSize = 14.sp)
    }
}

// Внутри NewActivity или в отдельном файле
data class SimulationFrame(
    val ants: List<Ant> = emptyList(),
    val spaces: List<CoworkingSpace> = emptyList(),
    val info: String = ""
)