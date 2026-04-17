package com.example.mobilka132

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilka132.data.ant.AntAlgorithm
import com.example.mobilka132.data.ant.CampusSimulation
import com.example.mobilka132.data.ant.CoworkingSpace
import com.example.mobilka132.data.ant.IntPoint
import com.example.mobilka132.model.SimulationFrame
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

    var currentSimulationFrame by mutableStateOf(SimulationFrame())

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
    val selectedTspBuildings = mutableStateListOf<Int>()
    val selectedDishes = mutableStateListOf<String>()

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

            if (Random.nextDouble() < 0.1) {
                selectedTspBuildings.add(color)
            }
        }
    }

    fun startCampusSimulation(buildingsMask: Bitmap, startOffset: Offset? = null) {
        if (activeJobs.isNotEmpty()) return
        val job = viewModelScope.launch {
            val coworkingSpaces = withContext(Dispatchers.Default) {
                extractCoworkingSpaces(buildingsMask)
            }
            val startPos = startOffset?.let { off ->
                IntPoint(
                    off.x.toInt().coerceIn(0, mapManager.width - 1),
                    off.y.toInt().coerceIn(0, mapManager.height - 1)
                )
            }
            var iterations = 0L
            val simulation = CampusSimulation(
                width = mapManager.width,
                height = mapManager.height,
                grid = mapManager.grid,
                studentCount = 100,
                customStartPosition = startPos,
                customSpaces = coworkingSpaces.ifEmpty { null }
            )
            val startTime = System.currentTimeMillis()
            while (isActive) {
                iterations += 1
                withContext(Dispatchers.Default) { simulation.update() }
                if (iterations % 5L == 0L)
                {
                    currentSimulationFrame = SimulationFrame(
                        ants = simulation.ants.toList(),
                        spaces = simulation.spaces.toList(),
                        info = "Time: ${(System.currentTimeMillis() - startTime) / 1000.0}s | " +
                                "Ants: ${simulation.ants.size} | Found: ${simulation.ants.count { it.hasFoundSpace }}"
                    )
                    delay(8)
                }
            }
        }
        activeJobs.add(job)
        job.invokeOnCompletion {
            currentSimulationFrame = SimulationFrame()
            activeJobs.remove(job)
        }
    }

    private fun extractCoworkingSpaces(buildingsMask: Bitmap): List<CoworkingSpace> {
        val bw = buildingsMask.width
        val bh = buildingsMask.height
        val pixels = IntArray(bw * bh)
        buildingsMask.getPixels(pixels, 0, bw, 0, 0, bw, bh)
        val result = mutableListOf<CoworkingSpace>()
        var id = 0
        CampusDatabase.getAllBuildings().forEach { (color, building) ->
            val coworkingVenues = building.venues.filter { it.isCoworking }
            if (coworkingVenues.isEmpty()) return@forEach
            var sumX = 0L; var sumY = 0L; var count = 0
            for (i in pixels.indices) {
                if ((pixels[i] and 0x00FFFFFF) == color) {
                    sumX += (i % bw); sumY += (i / bw); count++
                }
            }
            if (count == 0) return@forEach
            val walkable = findNearestWalkablePoint((sumX / count).toInt(), (sumY / count).toInt())
                ?: return@forEach
            for (venue in coworkingVenues) {
                result.add(CoworkingSpace(
                    id = id++,
                    position = IntPoint(walkable.x.toInt(), walkable.y.toInt()),
                    capacity = venue.coworkingCapacity,
                    comfort = venue.coworkingComfort
                ))
            }
        }
        return result
    }

    private fun findNearestWalkablePoint(cx: Int, cy: Int): Offset? {
        val grid = mapManager.grid
        val w = mapManager.width; val h = mapManager.height
        for (r in 0..40) {
            for (dy in -r..r) {
                for (dx in -r..r) {
                    if (kotlin.math.abs(dx) != r && kotlin.math.abs(dy) != r) continue
                    val nx = cx + dx; val ny = cy + dy
                    if (nx in 0 until w && ny in 0 until h && grid[ny * w + nx] == 1)
                        return Offset(nx.toFloat(), ny.toFloat())
                }
            }
        }
        return null
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

    fun toggleDish(dish: String) {
        if (selectedDishes.contains(dish)) {
            selectedDishes.remove(dish)
        } else {
            selectedDishes.add(dish)
        }
    }

    fun toggleTspBuilding(color: Int) {
        if (selectedTspBuildings.contains(color)) {
            selectedTspBuildings.remove(color)
        } else {
            selectedTspBuildings.add(color)
        }
    }

    fun selectAllTspBuildings(colors: Collection<Int>) {
        colors.forEach { color ->
            if (!selectedTspBuildings.contains(color)) {
                selectedTspBuildings.add(color)
            }
        }
    }

    fun clearTspBuildings(colors: Collection<Int>) {
        selectedTspBuildings.removeAll(colors)
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
                            val start =
                                if (parts.size >= 4) parts[2].trim().toIntOrNull() ?: 0 else 0
                            val end =
                                if (parts.size >= 4) parts[3].trim().toIntOrNull() ?: 1440 else 1440
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
    ) = requestPathfinding(
        p1.toPair(),
        p2.toPair(),
        visualizeSteps,
        stepDelay,
        onPathFound ?: ::onPathFoundCallback
    )

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

    private suspend fun getTspBuildingPoints(buildingsMask: Bitmap): List<Offset> = withContext(Dispatchers.Default) {
        val width = buildingsMask.width
        val height = buildingsMask.height
        if (width <= 0 || height <= 0) return@withContext emptyList<Offset>()

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

        val points = mutableListOf<Offset>()
        for (color in selectedTspBuildings) {
            val acc = buildingAccumulators[color] ?: continue
            val centroid = acc.centroid
            val walkablePoint = state.findNearestAvailablePoint(centroid)
            points.add(walkablePoint)
        }
        points
    }

    fun findTSPSolution(buildingsMask: Bitmap, startPoint: Offset? = null) {
        if (activeJobs.isNotEmpty()) return
        val ant = AntAlgorithm(walkableDistance)

        clear()
        tspPath = Path(emptyList(), 0f)
        isTSPProcessing = true
        val job = viewModelScope.launch {
            val buildingPoints = getTspBuildingPoints(buildingsMask).toMutableList()
            var actualStartIdx = -1

            withContext(pathfinderDispatcher) {
                if (buildingPoints.isNotEmpty()) {
                    if (startPoint != null) {
                        val snappedStart = state.findNearestAvailablePoint(startPoint)
                        buildingPoints.add(0, snappedStart)
                        actualStartIdx = 0
                    }
                    buildingPoints.forEach { state.addPoint(it) }
                    ant.setPoints(buildingPoints)

                    walkableDistance.setup(buildingPoints.map { Point(it.x.toInt(), it.y.toInt()) })
                } else {
                    val manualPoints = state.selectedPoints.map { it.position }
                    if (manualPoints.size >= 2) {
                        ant.setPoints(manualPoints)
                        walkableDistance.setup(manualPoints.map { Point(it.x.toInt(), it.y.toInt()) })
                    } else {
                        val n = 10
                        ant.generatePoints(mapManager.width, mapManager.height, n)
                        for (i in 0 until n) {
                            ant.points[i] = state.findNearestAvailablePoint(ant.points[i])
                            state.addPoint(ant.points[i])
                        }
                        walkableDistance.setup(ant.points.map { Point(it.x.toInt(), it.y.toInt()) })
                    }
                }

                ant.solve(
                    startNodeIndex = actualStartIdx,
                    onIteration = { _, _, dist ->
                        val steps = ant.getFullBestPathSteps()
                        viewModelScope.launch(Dispatchers.Main) {
                            if (steps.isNotEmpty()) {
                                tspPath = Path(steps, dist.toFloat())
                            }
                        }
                    }
                )
            }

            withContext(Dispatchers.Main) {
                val finalPath = tspPath?.let { if (it.steps.isNotEmpty()) Path(it.steps, it.distance) else null }
                isTSPProcessing = false
                if (finalPath != null) {
                    lastPath = finalPath
                    foundPaths.add(finalPath)
                }
                tspPath = null
            }
        }
        activeJobs.add(job)
        job.invokeOnCompletion {
            isTSPProcessing = false
            tspPath = null
            activeJobs.remove(job)
        }
    }

    fun startFoodShoppingGA(buildingsMask: Bitmap, userLocation: Offset? = null) {
        if (activeJobs.isNotEmpty()) return
        val job = viewModelScope.launch {
            isGARunning = true
            currentGeneration = 0
            totalGenerations = 200

            state.clearPoints()
            foundPaths.clear()
            tspPath = null
            lastPath = null
            currentStep = null
            currentGAStep = null

            val width = buildingsMask.width
            val height = buildingsMask.height

            if (width <= 0 || height <= 0) {
                isGARunning = false
                return@launch
            }

            var finalBestPath: Path? = null

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
                                buildingAccumulators.getOrPut(color) { CentroidAccumulator() }
                                    .add(x, y)
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
                    Log.d(
                        "GA_POINTS",
                        "Loaded ${venuePoints.size} venue points from CampusDatabase"
                    )
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

                userLocation?.let { loc ->
                    val snapped = state.findNearestAvailablePoint(loc)
                    state.selectedPoints.add(0, MapPoint(id = 0, position = snapped))
                }

                val mapPoints = state.selectedPoints.toList()
                if (mapPoints.size < 2) {
                    isGARunning = false
                    return@launch
                }

                val numPoints = mapPoints.size

                val targetDishes =
                    if (selectedDishes.isNotEmpty()) selectedDishes.toList() else mapPoints.flatMap { it.items }
                        .distinct()
                val dishToIndex = targetDishes.withIndex().associate { it.value to it.index }
                val numItems = targetDishes.size

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
                    mapPoints[i].items.mapNotNull { dishToIndex[it] }.toMutableList()
                }

                val calendar = Calendar.getInstance()
                val currentMinutes =
                    calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

                val ctx = MutationContext(
                    allPoints = (0 until numPoints).toMutableList(),
                    dist = walkableDistance,
                    items = items,
                    allItems = allItems,
                    initial = if (userLocation != null) 0 else Random.nextInt(0, numPoints),
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
                            val segments = mutableListOf<PathSegment>()
                            var totalDistance = 0.0
                            for (i in 0 until best.size - 1) {
                                val p1Idx = best[i]
                                val p2Idx = best[i + 1]
                                val segmentPoints = walkableDistance.path(p1Idx, p2Idx)
                                val dist = walkableDistance[p1Idx, p2Idx]

                                if (dist >= WalkableDistance.UNREACHABLE && p1Idx != p2Idx) {
                                    // Unreachable
                                    val start = walkableDistance.getPoint(p1Idx)
                                    val end = walkableDistance.getPoint(p2Idx)
                                    segments.add(
                                        PathSegment(
                                            Offset(start.x.toFloat(), start.y.toFloat()),
                                            Offset(end.x.toFloat(), end.y.toFloat()),
                                            false
                                        )
                                    )
                                } else {
                                    actualPath.addAll(segmentPoints.map {
                                        Offset(
                                            it.x.toFloat(),
                                            it.y.toFloat()
                                        )
                                    })
                                    totalDistance += dist
                                    if (segmentPoints.size >= 2) {
                                        for (j in 0 until segmentPoints.size - 1) {
                                            segments.add(
                                                PathSegment(
                                                    Offset(
                                                        segmentPoints[j].x.toFloat(),
                                                        segmentPoints[j].y.toFloat()
                                                    ),
                                                    Offset(
                                                        segmentPoints[j + 1].x.toFloat(),
                                                        segmentPoints[j + 1].y.toFloat()
                                                    ),
                                                    true
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            val path = Path(actualPath, totalDistance.toFloat(), segments)
                            finalBestPath = path
                            withContext(Dispatchers.Main) {
                                currentGAStep = GAStep(gen, path)
                                currentGeneration = gen
                            }
                        }
                        delay(10)
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isGARunning = false
                    finalBestPath?.let { path ->
                        lastPath = path
                        foundPaths.add(path)
                        Log.d(
                            "GA_FINISH",
                            "Saved path with ${path.segments.size} segments to foundPaths"
                        )
                    } ?: run {
                        Log.e("GA_FINISH", "No best path found to save")
                    }
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

        val centroid: Offset
            get() = if (count > 0) Offset(
                sumX.toFloat() / count,
                sumY.toFloat() / count
            ) else Offset.Zero
    }

    fun cancelAll() {
        for (j in activeJobs) {
            j.cancel()
        }
        isGARunning = false
        isPathProcessing = false
        isTSPProcessing = false
        currentStep = null
    }

    fun deletePoint(index: Int) {
        if (isProcessing) return
        if (index in state.selectedPoints.indices) {
            state.selectedPoints.removeAt(index)
        }
    }

    fun clear() {
        if (isProcessing) return

        foundPaths.clear()
        currentStep = null
        currentGAStep = null
        lastPath = null
        tspPath = null
        state.clearPoints()
        isGARunning = false
        isPathProcessing = false
        isTSPProcessing = false
    }

    fun clearResult() {
        if (isProcessing) return
        currentStep = null
        currentGAStep = null
        lastPath = null
        tspPath = null
        foundPaths.clear()
    }

    fun Pair<Int, Int>.toOffset() = Offset(first.toFloat(), second.toFloat())
    fun Offset.toPair() = Pair(x.toInt(), y.toInt())
    fun PathData.toPath(): Path =
        Path(path.map { n -> Offset(n.x.toFloat(), n.y.toFloat()) }, distance)
}
