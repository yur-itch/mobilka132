package com.example.mobilka132

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilka132.data.ant.AntColony
import com.example.mobilka132.data.genetic.*
import com.example.mobilka132.data.pathfinding.AStar
import com.example.mobilka132.data.pathfinding.PathData
import com.example.mobilka132.model.AStarStep
import com.example.mobilka132.model.GAStep
import com.example.mobilka132.model.MapPoint
import com.example.mobilka132.model.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random

class MapViewModel : ViewModel() {

    lateinit var mapManager: MapManager
    lateinit var pathfinder: AStar
    private lateinit var distancer: WalkableDistance

    val state = MapState()
    val overlay = MapOverlayRenderer(state)

    var foundPaths: MutableList<Path> = mutableStateListOf()
    var lastPath by mutableStateOf<Path?>(null)
    var currentStep by mutableStateOf<AStarStep?>(null)
    var currentGAStep by mutableStateOf<GAStep?>(null)

    var activeJobs: MutableList<Job> = mutableStateListOf()

    var isPathProcessing by mutableStateOf(false)
    var isGARunning by mutableStateOf(false)

    val isProcessing: Boolean
        get() = activeJobs.isNotEmpty() || !initialized

    val isAnyAlgoRunning: Boolean
        get() = isProcessing

    var initialized by mutableStateOf(false)

    var currentGeneration by mutableStateOf(0)
    var totalGenerations by mutableStateOf(0)

    val pathfinderDispatcher = Executors.newFixedThreadPool(3).asCoroutineDispatcher()

    private var loadedPointsWithTiming: List<MapPointData> = emptyList()

    fun init(mapManager: MapManager) {
        this.mapManager = mapManager
        state.init(mapManager.width, mapManager.height, mapManager.grid)
        pathfinder = AStar(mapManager.width, mapManager.height, mapManager.grid)
        distancer = WalkableDistance(pathfinder)
        initialized = true
    }

    fun loadPointsFromAssets(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val points = mutableListOf<MapPointData>()
                context.assets.open("ga_points.csv").bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.split(",")
                        if (parts.size >= 2) {
                            val x = parts[0].trim().toFloatOrNull()
                            val y = parts[1].trim().toFloatOrNull()
                            val start = if (parts.size >= 4) parts[2].trim().toIntOrNull() ?: 0 else 0
                            val end = if (parts.size >= 4) parts[3].trim().toIntOrNull() ?: 1440 else 1440
                            val d = if (parts.size >= 5) parts[4].trim().toIntOrNull() ?: 0 else 0

                            if (x != null && y != null) {
                                points.add(MapPointData(Offset(x, y), start, end, d))
                            }
                        }
                    }
                }
                loadedPointsWithTiming = points
                Log.d("GA_POINTS", "Successfully loaded ${points.size} points from assets")
            } catch (e: Exception) {
                Log.e("GA_POINTS", "Error loading points from assets", e)
            }
        }
    }

    fun onPointSelected(screenOffset: Offset, roadMask: Bitmap?, buildingsMask: Bitmap) {
        if (isProcessing) return

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
            foundPaths.add(path)
        }
    }

    fun requestPathfinding(visualizeSteps: Boolean = false) {
        if (activeJobs.isNotEmpty()) return
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
        stepDelay: Long = 5L,
        onPathFound: ((Boolean, Path) -> Unit)? = null
    ) = requestPathfinding(p1.position, p2.position, visualizeSteps, stepDelay, onPathFound)

    fun requestPathfinding(
        p1: Offset,
        p2: Offset,
        visualizeSteps: Boolean = false,
        stepDelay: Long = 5L,
        onPathFound: ((Boolean, Path) -> Unit)? = null
    ) = requestPathfinding(p1.toPair(), p2.toPair(), visualizeSteps, stepDelay, onPathFound ?: ::onPathFoundCallback)

    private fun requestPathfinding(
        start: Pair<Int, Int>,
        dest: Pair<Int, Int>,
        visualizeSteps: Boolean = false,
        stepDelay: Long = 5L,
        onPathFound: ((Boolean, Path) -> Unit)? = null
    ) {
        lastPath = null
        currentStep = null
        currentGAStep = null

        var foundPath: Path? = null
        isPathProcessing = true

        val pathJob = viewModelScope.launch {
            try {
                if (!visualizeSteps) {
                    withContext(pathfinderDispatcher) {
                        foundPath = pathfinder.find(start, dest).toPath()
                    }
                } else {
                    pathfinder.findPathAsync(start, dest, delayMs = stepDelay)
                        .flowOn(pathfinderDispatcher)
                        .collect { stepData ->
                            currentStep = AStarStep(
                                stepData.current.let { Offset(it.x.toFloat(), it.y.toFloat()) },
                                stepData.openSet.map { Offset(it.x.toFloat(), it.y.toFloat()) },
                                emptyList(),
                                stepData.path
                            )
                            foundPath = currentStep?.path
                        }
                }
            } catch (e: CancellationException) {
            } finally {
                isPathProcessing = false
                foundPath?.let {
                    val found = it.steps.isNotEmpty()
                    onPathFound?.invoke(found, it)
                } ?: onPathFound?.invoke(false, Path(emptyList(), 0f))
            }
        }
        activeJobs.add(pathJob)
        pathJob.invokeOnCompletion { cause ->
            if (cause is CancellationException) {
                currentStep = null
            }
            activeJobs.remove(pathJob)
        }
    }

    fun findTSPSolution() {
        if (activeJobs.isNotEmpty()) return
        val antColony = AntColony()
        val n = 10
        clear()

        val job = viewModelScope.launch {
            antColony.generatePoints(n)
            withContext(pathfinderDispatcher) {
                for (i in 0 until n) {
                    antColony.points[i] = state.findNearestAvailablePoint(antColony.points[i])
                    state.addPoint(antColony.points[i])
                }
                antColony.solve()
            }
            for (i in 0 until antColony.bestPath.size) {
                requestPathfinding(
                    antColony.points[antColony.bestPath[i]],
                    antColony.points[antColony.bestPath[(i + 1) % antColony.bestPath.size]]
                )
            }
        }
        activeJobs.add(job)
        job.invokeOnCompletion {
            activeJobs.remove(job)
        }
    }

    fun startFoodShoppingGA(maskBitmap: Bitmap) {
        if (activeJobs.isNotEmpty()) return
        val job = viewModelScope.launch {
            isGARunning = true
            currentGeneration = 0
            totalGenerations = 200

            state.clearPoints()
            lastPath = null
            currentStep = null
            currentGAStep = null

            val width = state.imageSize.width.toInt()
            val height = state.imageSize.height.toInt()

            if (width <= 0 || height <= 0) {
                isGARunning = false
                return@launch
            }

            try {
                if (loadedPointsWithTiming.isNotEmpty()) {
                    state.addPointsWithTiming(loadedPointsWithTiming)
                } else {
                    repeat(10) {
                        val randomPoint = Offset(
                            Random.nextInt(0, width).toFloat(),
                            Random.nextInt(0, height).toFloat()
                        )
                        state.addPoint(randomPoint)
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
                    Point(
                        x = it.position.x.toInt(),
                        y = it.position.y.toInt(),
                        workingStart = it.workingStart,
                        workingEnd = it.workingEnd,
                        delay = it.delay
                    )
                }
                distancer.setPoints(gaPoints)

                val allItems = (0 until numItems).toMutableList()
                val items = MutableList(numPoints) { mutableListOf<Int>() }
                allItems.forEach { item -> items[Random.nextInt(0, numPoints)].add(item) }

                val calendar = Calendar.getInstance()
                val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

                val ctx = MutationContext(
                    allPoints = (0 until numPoints).toMutableList(),
                    dist = distancer,
                    items = items,
                    allItems = allItems,
                    initial = Random.nextInt(0, numPoints),
                    startTime = currentMinutes,
                    speedKmh = 5.0,
                    metersPerPixel = 0.5
                )

                withContext(Dispatchers.Default) {
                    var population = newPopulation(50, ctx)
                    val totalGens = totalGenerations

                    for (gen in 1..totalGens) {
                        population = performGeneration(population, gen - 1, totalGens, ctx)

                        val best = population.maxByOrNull { fitness(it, ctx) }
                        if (best != null) {
                            val actualPath = mutableListOf<Offset>()
                            var distance = 0.0f
                            for (i in 0 until best.size - 1) {
                                val segment = distancer.path(best[i], best[i + 1])
                                actualPath.addAll(segment.map { Offset(it.x.toFloat(), it.y.toFloat()) })
                                distance += distancer[best[i], best[i + 1]].toFloat()
                            }

                            withContext(Dispatchers.Main) {
                                val path = Path(actualPath, distance)
                                currentGAStep = GAStep(gen, path)
                                currentGeneration = gen
                            }
                        }
                        delay(10)
                    }
                }
            } finally {
                isGARunning = false
                currentGAStep?.let {
                    lastPath = it.path
                    foundPaths.add(it.path)
                }
            }
        }
        activeJobs.add(job)
        job.invokeOnCompletion {
            activeJobs.remove(job)
        }
    }

    fun cancelAll() {
        for (j in activeJobs) {
            j.cancel()
        }
        isGARunning = false
        isPathProcessing = false
        currentStep = null
        currentGAStep = null
    }

    fun deletePoint(index: Int) {
        if (isProcessing) return

        if (index in state.selectedPoints.indices) {
            state.selectedPoints.removeAt(index)
            if (state.selectedPoints.size < 2) {
                lastPath = null
                currentGAStep = null
            }
        }
    }

    fun clear() {
        if (activeJobs.isNotEmpty()) return

        foundPaths.clear()
        currentStep = null
        currentGAStep = null
        lastPath = null
        state.clearPoints()
        isGARunning = false
        isPathProcessing = false
    }

    fun clearResult() {
        if (isProcessing) return
        currentStep = null
        currentGAStep = null
        lastPath = null
    }

    fun Pair<Int, Int>.toOffset() = Offset(first.toFloat(), second.toFloat())
    fun Offset.toPair() = Pair(x.toInt(), y.toInt())
    fun PathData.toPath(): Path = Path(path.map { n -> Offset(n.x.toFloat(), n.y.toFloat()) }, distance)
}