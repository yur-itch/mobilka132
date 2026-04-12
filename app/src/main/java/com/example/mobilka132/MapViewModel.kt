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

    lateinit var mapManager : MapManager
    lateinit var pathfinder: AStar
    lateinit var distancer: WalkableDistance
    val state = MapState()
    val overlay = MapOverlayRenderer(state)
    var foundPaths : MutableList<Path> = mutableListOf()
    var lastPath by mutableStateOf<Path?>(null)
    var currentStep by mutableStateOf<AStarStep?>(null)
    var activeJobs: MutableList<Job> = mutableStateListOf()

    var isPathProcessing by mutableStateOf(false)
    var isGARunning by mutableStateOf(false)

    val isProcessing: Boolean
        get() = activeJobs.isNotEmpty()

    var currentGeneration by mutableStateOf(0)
    var totalGenerations by mutableStateOf(0)

    val pathfinderDispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

    fun init(mapManager: MapManager) {
        this.mapManager = mapManager
        pathfinder = AStar(mapManager.grid)
        distancer = WalkableDistance(pathfinder)
    }

    fun onPointSelected(point: Offset, maskBitmap: Bitmap) {
        viewModelScope.launch {
            try {
                state.addPoint(point, maskBitmap)
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
        if (activeJobs.isNotEmpty()) return
        val points = state.selectedPoints.toList()
        if (points.size >= 2) {
            val p1 = points[points.size - 2]
            val p2 = points[points.size - 1]
            requestPathfinding(p1, p2, visualizeSteps)
        }
    }

    fun requestPathfinding(p1: MapPoint, p2: MapPoint, visualizeSteps: Boolean = false, stepDelay: Long = 5L, onPathFound: ((Boolean, Path) -> Unit)? = null) =
        requestPathfinding(p1.position, p2.position, visualizeSteps, stepDelay, onPathFound)

    fun requestPathfinding(p1: Offset, p2: Offset, visualizeSteps: Boolean = false, stepDelay: Long = 5L, onPathFound: ((Boolean, Path) -> Unit)? = null) =
        requestPathfinding(p1.toPair(), p2.toPair(), visualizeSteps, stepDelay, onPathFound ?: ::onPathFoundCallback)

    private fun requestPathfinding(start: Pair<Int, Int>, dest: Pair<Int, Int>,
                                   visualizeSteps: Boolean = false,
                                   stepDelay: Long = 5L,
                                   onPathFound: ((Boolean, Path) -> Unit)? = null) {
        if (activeJobs.isNotEmpty()) return
        lastPath = null
        currentStep = null

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
                                stepData.closedSet.map { Offset(it.x.toFloat(), it.y.toFloat())},
                                stepData.path?.toPath()
                            )
                        }
                }
            } catch (e: CancellationException) {
            } finally {
                isPathProcessing = false
                foundPath?.let {
                    val found = !it.steps.isEmpty()
                    if (found) {
                        foundPaths.add(it)
                    }
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

    fun startFoodShoppingGA(maskBitmap: Bitmap) {
        if (activeJobs.isNotEmpty()) return
        val job = viewModelScope.launch {
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
                if (mapManager.loadedPoints.isNotEmpty()) {
                    state.addPointsDirectly(mapManager.loadedPoints)
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

                val gaPoints = mapPoints.map { Point(it.position.x.toInt(), it.position.y.toInt()) }
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
    }

    fun deletePoint(index: Int) {
        if (index in state.selectedPoints.indices) {
            state.selectedPoints.removeAt(index)
        }
    }

    fun clear() {
        if (activeJobs.isNotEmpty()) return
        currentStep = null
        lastPath = null
        state.clearPoints()
        isGARunning = false
        isPathProcessing = false
    }

    fun Pair<Int, Int>.toOffset() = Offset(first.toFloat(), second.toFloat())
    fun Offset.toPair() = Pair<Int, Int>(x.toInt(), y.toInt())
    fun PathData.toPath(): Path = Path(path.map { n -> Offset(n.x.toFloat(), n.y.toFloat()) }, distance)
}