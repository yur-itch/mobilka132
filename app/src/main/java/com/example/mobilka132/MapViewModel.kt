package com.example.mobilka132

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilka132.data.ant.AntAlgorithm
import com.example.mobilka132.data.genetic.*
import com.example.mobilka132.data.pathfinding.AStar
import com.example.mobilka132.data.pathfinding.PathData
import com.example.mobilka132.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar
import java.util.concurrent.Executors
import kotlin.random.Random

class MapViewModel : ViewModel() {

    lateinit var mapManager: MapManager
    lateinit var pathfinder: AStar
    lateinit var walkableDistance: WalkableDistance

    val state = MapState()
    val overlay = MapOverlayRenderer(state)

    var foundPaths: MutableList<Path> = mutableStateListOf()
    var lastPath by mutableStateOf<Path?>(null)
    var currentStep by mutableStateOf<AStarStep?>(null)
    var currentGAStep by mutableStateOf<GAStep?>(null)
    var tspPath by mutableStateOf<Path?>(null)
    var activeJobs: MutableList<Job> = mutableStateListOf()

    var isPathProcessing by mutableStateOf(false)
    var isGARunning by mutableStateOf(false)
    var isTSPProcessing by mutableStateOf(false)
    val isProcessing: Boolean
        get() = activeJobs.isNotEmpty() || !initialized

    val isAnyAlgoRunning: Boolean
        get() = isProcessing

    var obstacles = mutableStateListOf<ObstacleLine>()
    var isObstacleMode by mutableStateOf(false)

    var initialized by mutableStateOf(false)

    var currentGeneration by mutableIntStateOf(0)
    var totalGenerations by mutableIntStateOf(0)

    val pathfinderDispatcher = Executors.newFixedThreadPool(3).asCoroutineDispatcher()

    private var loadedPointsWithTiming: List<MapPointData> = emptyList()

    val selectedVenues = mutableStateMapOf<Int, Set<String>>()

    fun init(mapManager: MapManager) {
        this.mapManager = mapManager
        state.init(mapManager.width, mapManager.height, mapManager.grid)
        pathfinder = AStar(mapManager.width, mapManager.height, mapManager.grid, state)
        walkableDistance = WalkableDistance(pathfinder)
        initialized = true

        CampusDatabase.getAllBuildings().forEach { (color, building) ->
            val selected = building.venues
                .filter { Random.nextDouble() < 0.25 }
                .map { it.name }
                .toSet()
            
            if (selected.isNotEmpty()) {
                selectedVenues[color] = selected
            }
        }
    }

    fun toggleVenue(buildingColor: Int, venueName: String) {
        val currentSet = selectedVenues[buildingColor]?.toMutableSet() ?: mutableSetOf()
        if (currentSet.contains(venueName)) {
            currentSet.remove(venueName)
        } else {
            currentSet.add(venueName)
        }
        selectedVenues[buildingColor] = currentSet
    }

    fun setBuildingVenues(buildingColor: Int, selected: Boolean) {
        val building = CampusDatabase.getBuildingByColor(buildingColor) ?: return
        if (selected) {
            selectedVenues[buildingColor] = building.venues.map { it.name }.toSet()
        } else {
            selectedVenues[buildingColor] = emptySet()
        }
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
            } catch (_: Exception) {
                Log.e("GA_POINTS", "Error loading points from assets")
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
                            currentStep = stepData
                            foundPath = stepData.path
                        }
                }
            } catch (_: CancellationException) {
            } finally {
                isPathProcessing = false
                val resultPath = foundPath ?: Path(emptyList(), 0f)
                onPathFound?.invoke(resultPath.steps.isNotEmpty(), resultPath)
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
        val ant = AntAlgorithm(pathfinder)
        val n = 10
        clear()
        tspPath = Path(emptyList(), 0f)
        isTSPProcessing = true
        val job = viewModelScope.launch {
            ant.generatePoints(mapManager.width, mapManager.height, n)
            withContext(pathfinderDispatcher) {
                for (i in 0 until n) {
                    ant.points[i] = state.findNearestAvailablePoint(ant.points[i])
                    state.addPoint(ant.points[i])
                }
                ant.solve(
                    onIteration = { _, _, dist ->
                        val steps = ant.getFullBestPathSteps()
                        viewModelScope.launch(Dispatchers.Main) {
                            tspPath = Path(steps, dist.toFloat())
                        }
                    }
                )
            }

            withContext(Dispatchers.Main) {
                val finalPath = Path(tspPath!!.steps, tspPath!!.distance)
                isTSPProcessing = false
                lastPath = finalPath
                foundPaths.add(finalPath)
            }
        }
        activeJobs.add(job)
        job.invokeOnCompletion {
            isTSPProcessing = false
            tspPath = null
            activeJobs.remove(job)
        }
    }

    fun startFoodShoppingGA(buildingsMask: Bitmap) {
        if (activeJobs.isNotEmpty()) return
        val job = viewModelScope.launch {
            isGARunning = true
            currentGeneration = 0
            totalGenerations = 200

            state.clearPoints()
            lastPath = null
            currentStep = null
            currentGAStep = null

            val width = buildingsMask.width
            val height = buildingsMask.height

            if (width <= 0 || height <= 0) {
                isGARunning = false
                return@launch
            }

            try {
                val venuePoints = withContext(Dispatchers.Default) {
                    val pixels = IntArray(width * height)
                    buildingsMask.getPixels(pixels, 0, width, 0, 0, width, height)

                    val buildingAccumulators = mutableMapOf<Int, CentroidAccumulator>()
                    val registeredColors = CampusDatabase.getAllBuildings().keys

                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            val color = pixels[y * width + x] and 0x00FFFFFF
                            if (color in registeredColors) {
                                buildingAccumulators.getOrPut(color) { CentroidAccumulator() }.add(x, y)
                            }
                        }
                    }

                    val points = mutableListOf<MapPointData>()
                    for ((color, acc) in buildingAccumulators) {
                        val building = CampusDatabase.getBuildingByColor(color) ?: continue
                        if (building.venues.isEmpty()) continue

                        val selectedInBuilding = selectedVenues[color] ?: emptySet()
                        if (selectedInBuilding.isEmpty()) continue

                        val centroid = acc.centroid
                        val walkablePoint = state.findNearestAvailablePoint(centroid)

                        for (venue in building.venues) {
                            if (venue.name !in selectedInBuilding) continue

                            val hours = venue.workingHours.split("-")
                            val start = if (hours.size == 2) parseTime(hours[0]) else 0
                            val end = if (hours.size == 2) parseTime(hours[1]) else 1440
                            points.add(
                                MapPointData(
                                    walkablePoint,
                                    start,
                                    end,
                                    venue.estimatedVisitTimeMinutes,
                                    venue.dishes
                                )
                            )
                        }
                    }
                    points
                }

                if (venuePoints.isNotEmpty()) {
                    state.addPointsWithTiming(venuePoints)
                    Log.d("GA_POINTS", "Loaded ${venuePoints.size} venue points from CampusDatabase")
                } else {
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
                }

                val mapPoints = state.selectedPoints.toList()
                if (mapPoints.size < 2) {
                    isGARunning = false
                    return@launch
                }

                val numPoints = mapPoints.size
                
                val uniqueMenuItems = mapPoints.flatMap { it.items }.distinct()
                val menuItemToIndex = uniqueMenuItems.withIndex().associate { it.value to it.index }
                val numItems = uniqueMenuItems.size

                val gaPoints = mapPoints.map {
                    Point(
                        x = it.position.x.toInt(),
                        y = it.position.y.toInt(),
                        workingStart = it.workingStart,
                        workingEnd = it.workingEnd,
                        delay = it.delay
                    )
                }
                
                walkableDistance.setup(gaPoints)

                val allItems = (0 until numItems).toMutableList()
                val items = MutableList(numPoints) { i -> 
                    mapPoints[i].items.mapNotNull { menuItemToIndex[it] }.toMutableList()
                }

                val calendar = Calendar.getInstance()
                val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

                val ctx = MutationContext(
                    allPoints = (0 until numPoints).toMutableList(),
                    dist = walkableDistance,
                    items = items,
                    allItems = allItems,
                    initial = Random.nextInt(0, numPoints),
                    startTime = currentMinutes,
                    speedKmh = 5.0,
                    metersPerPixel = state.metersPerPixel
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
                                val segment = walkableDistance.path(best[i], best[i + 1])
                                actualPath.addAll(segment.map { Offset(it.x.toFloat(), it.y.toFloat()) })
                                distance += walkableDistance[best[i], best[i + 1]].toFloat()
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
            currentGAStep = null
            activeJobs.remove(job)
        }
    }

    fun addObstacle(line: ObstacleLine) {
        obstacles.add(line)
        syncObstacles()
    }

    fun removeObstacle(id: Int) {
        obstacles.removeAll { it.id == id }
        syncObstacles()
    }

    fun clearObstacles() {
        obstacles.clear()
        syncObstacles()
    }

    fun syncObstacles() {
        mapManager.updateObstacles(obstacles)
        if (::walkableDistance.isInitialized) {
            walkableDistance.clearPersistentCache()
        }
    }

    private fun parseTime(timeStr: String): Int {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size == 2) {
                parts[0].toInt() * 60 + parts[1].toInt()
            } else 0
        } catch (_: Exception) {
            0
        }
    }

    class CentroidAccumulator {
        var sumX = 0L
        var sumY = 0L
        var count = 0
        fun add(x: Int, y: Int) {
            sumX += x.toLong()
            sumY += y.toLong()
            count++
        }
        val centroid: Offset get() = if (count > 0) Offset(sumX.toFloat() / count, sumY.toFloat() / count) else Offset.Zero
    }

    fun cancelAll() {
        for (j in activeJobs) {
            j.cancel()
        }
        isGARunning = false
        isPathProcessing = false
        currentStep = null
    }

    fun deletePoint(index: Int) {
        if (isProcessing) return
        if (index in state.selectedPoints.indices) {
            state.selectedPoints.removeAt(index)
        }
    }

    fun clear() {
        if (activeJobs.isNotEmpty()) return

        foundPaths.clear()
        currentStep = null
        currentGAStep = null
        lastPath = null
        tspPath = null
        state.clearPoints()
        isGARunning = false
        isPathProcessing = false
    }

    fun clearResult() {
        if (isProcessing) return
        currentStep = null
        currentGAStep = null
        lastPath = null
        tspPath = null
    }

    fun Pair<Int, Int>.toOffset() = Offset(first.toFloat(), second.toFloat())
    fun Offset.toPair() = Pair(x.toInt(), y.toInt())
    fun PathData.toPath(): Path = Path(path.map { n -> Offset(n.x.toFloat(), n.y.toFloat()) }, distance)
}
