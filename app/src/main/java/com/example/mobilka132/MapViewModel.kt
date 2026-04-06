package com.example.mobilka132

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilka132.data.genetic.*
import com.example.mobilka132.data.pathfinding.AStar
import com.example.mobilka132.data.pathfinding.AStarStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections.emptyList
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

class MapViewModel : ViewModel() {

    lateinit var algorithm: AStar
    private lateinit var distancer: WalkableDistance
    val state = MapState()
    val overlay = MapOverlayRenderer(state)
    var lastPath by mutableStateOf<List<Offset>>(emptyList())
    var currentStep by mutableStateOf<AStarStep?>(null)
    private var pathJob: Job? = null

    var isPathProcessing by mutableStateOf(false)
    var isGARunning by mutableStateOf(false)
    val isAnyAlgoRunning get() = isPathProcessing || isGARunning

    var currentGeneration by mutableStateOf(0)
    var totalGenerations by mutableStateOf(0)

    private var loadedPoints: List<Offset> = emptyList()

    fun init(grid: Array<Array<Int>>) {
        algorithm = AStar(grid)
        distancer = WalkableDistance(algorithm)
    }

    fun loadPointsFromAssets(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val points = mutableListOf<Offset>()
                context.assets.open("ga_points.csv").bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.split(",")
                        if (parts.size == 2) {
                            val x = parts[0].trim().toFloatOrNull()
                            val y = parts[1].trim().toFloatOrNull()
                            if (x != null && y != null) {
                                points.add(Offset(x, y))
                            }
                        }
                    }
                }
                loadedPoints = points
                Log.d("GA_POINTS", "Successfully loaded ${points.size} points from assets")
            } catch (e: Exception) {
                Log.e("GA_POINTS", "Error loading points from assets", e)
            }
        }
    }

    fun onPointSelected(point: Offset, maskBitmap: Bitmap) {
        if (isAnyAlgoRunning) return
        viewModelScope.launch {
            try {
                state.addPoint(point, maskBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun requestPathfinding(visualizeSteps : Boolean = false){
        if (isAnyAlgoRunning) return
        
        val points = state.selectedPoints.toList()
        if (points.size < 2) return

        pathJob?.cancel()
        // Ensure the screen is cleared of any previous algorithmic results (GA or A*)
        lastPath = emptyList()
        currentStep = null
        
        try {
            isPathProcessing = true
            val p1 = points[points.size - 2]
            val p2 = points[points.size - 1]
            if (!visualizeSteps)
            {
                pathJob = viewModelScope.launch {
                    try {
                        val path = withContext(Dispatchers.Default) {
                            algorithm.findPath(p1.position.toPair(),p2.position.toPair()).map { p -> p.toOffset() }
                        }
                        lastPath = path
                    } finally {
                        isPathProcessing = false
                    }
                }
            }
            else
            {
                startPathfinding(p1.position.toPair(), p2.position.toPair())
            }
        }
        catch (e: Exception) {
            isPathProcessing = false
        }
    }

    fun cancelAll() {
        pathJob?.cancel()
        currentStep = null
        lastPath = emptyList()
        isGARunning = false
        isPathProcessing = false
    }

    fun cancelPathfinding(){
        cancelAll()
    }

    fun startPathfinding(start: Pair<Int, Int>, end: Pair<Int, Int>) {
        pathJob?.cancel()

        pathJob = viewModelScope.launch {
            try {
                isPathProcessing = true
                algorithm.findPathAsync(start, end, delayMs = 5)
                    .flowOn(Dispatchers.Default)
                    .collect { step ->
                        currentStep = step
                        step.path?.let { path ->
                            lastPath = path.map { pair ->
                                pair.toOffset()
                            }
                        }
                    }

            } catch (e: CancellationException) {
            } finally {
                isPathProcessing = false
            }
        }
    }

    fun startFoodShoppingGA(maskBitmap: Bitmap) {
        if (isAnyAlgoRunning) return
        pathJob?.cancel()
        pathJob = viewModelScope.launch {
            isGARunning = true
            currentGeneration = 0
            totalGenerations = 200

            state.clearPoints()
            lastPath = emptyList()
            currentStep = null

            val width = state.imageSize.width.toInt()
            val height = state.imageSize.height.toInt()

            if (width <= 0 || height <= 0) {
                isGARunning = false
                return@launch
            }

            try {
                if (loadedPoints.isNotEmpty()) {
                    // Part 2: Added directly without snapping
                    state.addPointsDirectly(loadedPoints)
                } else {
                    repeat(10) {
                        val randomPoint = Offset(
                            Random.nextInt(0, width).toFloat(),
                            Random.nextInt(0, height).toFloat()
                        )
                        state.addPoint(randomPoint, maskBitmap)
                    }
                }

                val mapPoints = state.selectedPoints.toList()
                if (mapPoints.size < 2) {
                    isGARunning = false
                    return@launch
                }

                val numPoints = mapPoints.size
                val numItems = 10

                val gaPoints = mapPoints.map { com.example.mobilka132.data.genetic.Point(it.position.x.toInt(), it.position.y.toInt()) }
                distancer.setPoints(gaPoints)

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

                withContext(Dispatchers.Default) {
                    var population = newPopulation(50, ctx)
                    val totalGens = totalGenerations

                    for (gen in 1..totalGens) {
                        population = performGeneration(population, gen - 1, totalGens, ctx)

                        val best = population.maxByOrNull { fitness(it, ctx) }
                        if (best != null) {
                            val actualPath = mutableListOf<Offset>()
                            for (i in 0 until best.size - 1) {
                                val segment = distancer.path(best[i], best[i+1])
                                actualPath.addAll(segment.map { Offset(it.x.toFloat(), it.y.toFloat()) })
                            }
                            // Update UI on main thread
                            withContext(Dispatchers.Main) {
                                lastPath = actualPath
                                currentGeneration = gen
                            }
                        }
                        delay(10)
                    }
                }
            } finally {
                isGARunning = false
            }
        }
    }

    fun deletePoint(index: Int) {
        if (isAnyAlgoRunning) return
        if (index in state.selectedPoints.indices) {
            state.selectedPoints.removeAt(index)
            lastPath = emptyList()
        }
    }

    fun clear() {
        if (isAnyAlgoRunning) return
        state.clearPoints()
        currentStep = null
        lastPath = emptyList()
        isGARunning = false
        isPathProcessing = false
    }

    fun clearResult() {
        if (isAnyAlgoRunning) return
        currentStep = null
        lastPath = emptyList()
    }

    fun Pair<Int, Int>.toOffset() = Offset(first.toFloat(), second.toFloat())
    fun Offset.toPair() = Pair<Int, Int>(x.toInt(), y.toInt())
}
