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
import com.example.mobilka132.data.pathfinding.PathData
import com.example.mobilka132.model.AStarStep
import com.example.mobilka132.model.MapPoint
import com.example.mobilka132.model.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections.emptyList
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

class MapViewModel : ViewModel() {

    lateinit var algorithm: AStar
    private lateinit var distancer: WalkableDistance

    val state = MapState()
    val overlay = MapOverlayRenderer(state)

    var lastPath by mutableStateOf<Path?>(null)
    var currentStep by mutableStateOf<AStarStep?>(null)

    private var pathJob: Job? = null

    var isPathProcessing by mutableStateOf(false)
    var isGARunning by mutableStateOf(false)
    val isAnyAlgoRunning get() = isPathProcessing || isGARunning

    var currentGeneration by mutableStateOf(0)
    var totalGenerations by mutableStateOf(0)

    val pathfinderDispatcher = Executors.newFixedThreadPool(3).asCoroutineDispatcher()

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

    /**
     * UPDATED SIGNATURE (incoming)
     * Now supports buildings mask detection.
     */
    fun onPointSelected(screenOffset: Offset, roadMask: Bitmap, buildingsMask: Bitmap) {
        if (isAnyAlgoRunning) return

        viewModelScope.launch {
            try {
                val contentPoint = state.screenToContent(screenOffset)
                state.handleMapClick(contentPoint, roadMask, buildingsMask)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onPathFoundCallback(found: Boolean, path: Path) {
        if (found) {
            lastPath = path
        }
    }

    fun requestPathfinding(visualizeSteps: Boolean = false) {
        if (isAnyAlgoRunning) return
        val points = state.selectedPoints.toList()
        if (points.size >= 2) {
            val p1 = points[points.size - 2]
            val p2 = points[points.size - 1]
            requestPathfinding(p1, p2, visualizeSteps)
        }
    }

    fun requestPathfinding(
        p1: MapPoint,
        p2: MapPoint,
        visualizeSteps: Boolean = false,
        onPathFound: ((Boolean, Path) -> Unit)? = null
    ) = requestPathfinding(p1.position, p2.position, visualizeSteps, onPathFound)

    fun requestPathfinding(
        p1: Offset,
        p2: Offset,
        visualizeSteps: Boolean = false,
        onPathFound: ((Boolean, Path) -> Unit)? = null
    ) = requestPathfinding(p1.toPair(), p2.toPair(), visualizeSteps, onPathFound ?: ::onPathFoundCallback)

    private fun requestPathfinding(
        start: Pair<Int, Int>,
        dest: Pair<Int, Int>,
        visualizeSteps: Boolean = false,
        onPathFound: ((Boolean, Path) -> Unit)? = null
    ) {
        if (isAnyAlgoRunning) return

        pathJob?.cancel()
        lastPath = null
        currentStep = null

        var foundPath: PathData? = null
        isPathProcessing = true

        pathJob = viewModelScope.launch {
            try {
                if (!visualizeSteps) {
                    withContext(pathfinderDispatcher) {
                        foundPath = algorithm.find(start, dest)
                    }
                } else {
                    algorithm.findPathAsync(start, dest, delayMs = 5)
                        .flowOn(pathfinderDispatcher)
                        .collect { step ->
                            currentStep = step
                            step.path?.let { pathObject ->
                                foundPath = PathData(
                                    pathObject.steps.map { offset ->
                                        com.example.mobilka132.data.pathfinding.Node(
                                            offset.x.toInt(),
                                            offset.y.toInt(),
                                            0
                                        )
                                    },
                                    pathObject.distance
                                )
                            }
                        }
                }
            } catch (e: CancellationException) {
            } finally {
                isPathProcessing = false
                foundPath?.let {
                    onPathFound?.invoke(it.path.isNotEmpty(), it.toPath())
                } ?: onPathFound?.invoke(false, Path(emptyList(), 0f))
            }
        }
    }

    fun startFoodShoppingGA(maskBitmap: Bitmap) {
        if (isAnyAlgoRunning) return

        viewModelScope.launch {
            isGARunning = true
            currentGeneration = 0
            totalGenerations = 200

            state.clearPoints()
            lastPath = null
            currentStep = null

            val width = state.imageSize.width.toInt()
            val height = state.imageSize.height.toInt()

            if (width <= 0 || height <= 0) {
                isGARunning = false
                return@launch
            }

            try {
                if (loadedPoints.isNotEmpty()) {
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

                val gaPoints = mapPoints.map {
                    com.example.mobilka132.data.genetic.Point(
                        it.position.x.toInt(),
                        it.position.y.toInt()
                    )
                }
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
                                val segment = distancer.path(best[i], best[i + 1])
                                actualPath.addAll(segment.map { Offset(it.x.toFloat(), it.y.toFloat()) })
                            }

                            withContext(Dispatchers.Main) {
                                lastPath = Path(actualPath, 0f)
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

    fun cancelAll() {
        pathJob?.cancel()
        currentStep = null
        lastPath = null
        isGARunning = false
        isPathProcessing = false
    }

    fun cancelPathfinding() {
        cancelAll()
    }

    fun deletePoint(index: Int) {
        if (isAnyAlgoRunning) return

        if (index in state.selectedPoints.indices) {
            state.selectedPoints.removeAt(index)
            if (state.selectedPoints.size < 2) {
                lastPath = null
            }
        }
    }

    fun clear() {
        if (isAnyAlgoRunning) return

        state.clearPoints()
        currentStep = null
        lastPath = null
        isGARunning = false
        isPathProcessing = false
    }

    fun clearResult() {
        if (isAnyAlgoRunning) return
        currentStep = null
        lastPath = null
    }

    fun Pair<Int, Int>.toOffset() = Offset(first.toFloat(), second.toFloat())
    fun Offset.toPair() = Pair(x.toInt(), y.toInt())
    fun PathData.toPath(): Path = Path(path.map { n -> Offset(n.x.toFloat(), n.y.toFloat()) }, distance)
}