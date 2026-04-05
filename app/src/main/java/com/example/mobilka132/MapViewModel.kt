package com.example.mobilka132

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilka132.data.pathfinding.AStar
import com.example.mobilka132.model.AStarStep
import com.example.mobilka132.data.pathfinding.PathData
import com.example.mobilka132.model.MapPoint
import com.example.mobilka132.model.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections.emptyList
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException

class MapViewModel : ViewModel() {

    lateinit var algorithm: AStar
    val state = MapState()
    val overlay = MapOverlayRenderer(state)
    var lastPath by mutableStateOf<Path?>(null)
    var currentStep by mutableStateOf<AStarStep?>(null)
    private var pathJob: Job? = null

    var isPathProcessing by mutableStateOf(false)

    val pathfinderDispatcher = Executors.newFixedThreadPool(3).asCoroutineDispatcher()

    fun init(grid: Array<Array<Int>>) {
        algorithm = AStar(grid)
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

    fun onPathFoundCallback(found : Boolean, path : Path) {
        if (found) {
            lastPath = path
        }
    }

    fun requestPathfinding(visualizeSteps : Boolean = false) {
        val points = state.selectedPoints.toList()
        if (points.size >= 2) {
            val p1 = points[points.size - 2]
            val p2 = points[points.size - 1]
            requestPathfinding(p1, p2, visualizeSteps)
        }
    }
    fun requestPathfinding(p1 : MapPoint, p2 : MapPoint, visualizeSteps : Boolean = false, onPathFound: ((Boolean, Path) -> Unit)? = null) =
        requestPathfinding(p1.position, p2.position, visualizeSteps, onPathFound)

    fun requestPathfinding(p1 : Offset, p2 : Offset, visualizeSteps : Boolean = false, onPathFound: ((Boolean, Path) -> Unit)? = null) =
            requestPathfinding(p1.toPair(), p2.toPair(), visualizeSteps, onPathFound ?: ::onPathFoundCallback)

    private fun requestPathfinding(start : Pair<Int, Int>, dest : Pair<Int, Int>, visualizeSteps : Boolean = false, onPathFound: ((Boolean, Path) -> Unit)? = null){
        var foundPath : PathData? = null
        try {
            if (!visualizeSteps)
            {
                pathJob = viewModelScope.launch {
                    withContext(pathfinderDispatcher) {
                        foundPath = algorithm.find(start, dest)
                    }
                }
            }
            else
            {
                pathJob = viewModelScope.launch {
                    try {
                        algorithm.findPathAsync(start, dest, delayMs = 5)
                            .flowOn(pathfinderDispatcher)
                            .collect { step ->
                                currentStep = AStarStep(
                                    step.current.let { n -> Pair(n.x, n.y)},
                                    step.openSet.map { n -> Pair(n.x, n.y) },
                                    step.closedSet.map { n -> Pair(n.x, n.y)}
                                    )
                                foundPath = step.path
                            }
                    } catch (e: CancellationException) {
                    } finally {
                    }
                }
            }
            isPathProcessing = true;
            pathJob?.invokeOnCompletion { cause ->
                if (cause == null) {
                    foundPath?.let { onPathFound?.invoke(!it.path.isEmpty(), it.toPath()) }
                }
                onPathFound?.invoke(false, Path(emptyList(), 0f))
                isPathProcessing = false
            }
        }
        finally {
        }
    }

    fun cancelPathfinding(){
        pathJob?.cancel()
        currentStep = null;
        lastPath = null
    }

    fun deletePoint(index: Int) {
        if (index in state.selectedPoints.indices) {
            state.selectedPoints.removeAt(index)

            if (state.selectedPoints.size < 2) {
                lastPath = null
            }
        }
    }

    fun clear() {
        state.clearPoints()
        currentStep = null;
        lastPath = null
    }

    fun Pair<Int, Int>.toOffset() = Offset(first.toFloat(), second.toFloat())
    fun Offset.toPair() = Pair<Int, Int>(x.toInt(), y.toInt())
    fun PathData.toPath() : Path = Path(path.map { n -> Offset(n.x.toFloat(), n.y.toFloat())}, distance)
}