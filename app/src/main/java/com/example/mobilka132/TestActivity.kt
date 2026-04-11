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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random
import com.example.mobilka132.data.genetic.*

class TestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var currentGen by remember { mutableStateOf(0) }
            var bestDist by remember { mutableStateOf(0.0) }
            var currentBestRoute by remember { mutableStateOf<List<Int>>(emptyList()) }
            var pointsList by remember { mutableStateOf<List<Point>>(emptyList()) }

            LaunchedEffect(Unit) {
                val numPoints = 30
                val numItems = 10
                val points = List(numPoints) { Point(Random.nextInt(0, 1000), Random.nextInt(0, 1000)) }
                pointsList = points

                val distancer = EucledianDistance(points)
                val allItems = (0 until numItems).toMutableList()
                val items = MutableList(numPoints) { mutableListOf<Int>() }

                allItems.forEach { item -> items[Random.nextInt(0, numPoints)].add(item) }

                val ctx = MutationContext(
                    allPoints = (0 until numPoints).toMutableList(),
                    dist = distancer,
                    items = items,
                    allItems = allItems,
                    initial = Random.nextInt(0, numPoints)
                )

                var population = newPopulation(50, ctx)

                val totalGens = 200
                for (gen in 1..totalGens) {
                    // Run logic on background thread
                    val nextPop = withContext(Dispatchers.Default) {
                        performGeneration(population, gen - 1, totalGens, ctx)
                    }
                    population = nextPop

                    val best = population.maxByOrNull { fitness(it, ctx) }
                    if (best != null) {
                        currentBestRoute = best.toList()
                        bestDist = (1 until best.size).sumOf { distancer[best[it - 1], best[it]] }
                    }

                    currentGen = gen
                    //delay(50)
                }
            }

            Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1E))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Generation: $currentGen", color = Color.White)
                    Text("Distance: ${bestDist.toInt()}", color = Color(0xFF00FF7F))
                }

                Box(modifier = Modifier.weight(1f)) {
                    VisualizerCanvas(pointsList, currentBestRoute)
                }
            }
        }
    }
}

@Composable
fun VisualizerCanvas(points: List<Point>, route: List<Int>) {
    Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        val canvasW = size.width
        val canvasH = size.height

        fun scaleX(x: Int) = (x.toFloat() / 1000f) * canvasW
        fun scaleY(y: Int) = (y.toFloat() / 1000f) * canvasH

        points.forEach { pt ->
            drawCircle(
                color = Color.Gray.copy(alpha = 0.5f),
                radius = 5f,
                center = Offset(scaleX(pt.x), scaleY(pt.y))
            )
        }

        if (route.isNotEmpty()) {
            for (i in 0 until route.size - 1) {
                val p1 = points[route[i]]
                val p2 = points[route[i+1]]
                drawLine(
                    color = Color(0xFF00FF7F),
                    start = Offset(scaleX(p1.x), scaleY(p1.y)),
                    end = Offset(scaleX(p2.x), scaleY(p2.y)),
                    strokeWidth = 4f
                )
            }

            route.forEachIndexed { index, pointIdx ->
                val pt = points[pointIdx]
                drawCircle(
                    color = if (index == 0) Color.Red else Color.White,
                    radius = if (index == 0) 12f else 8f,
                    center = Offset(scaleX(pt.x), scaleY(pt.y))
                )
            }
        }
    }
}