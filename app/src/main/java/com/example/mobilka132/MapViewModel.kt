package com.example.mobilka132

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilka132.data.pathfinding.AStar
import com.example.mobilka132.data.pathfinding.AStarStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections.emptyList
import kotlin.coroutines.cancellation.CancellationException

class MapViewModel : ViewModel() {

    lateinit var algorithm: AStar
    val state = MapState()
    val overlay = MapOverlayRenderer(state)
    var lastPath by mutableStateOf<List<Offset>>(emptyList())
    var currentStep by mutableStateOf<AStarStep?>(null)
    private var pathJob: Job? = null

    var isPathProcessing by mutableStateOf(false)

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

    fun requestPathfinding(visualizeSteps : Boolean = false){
        pathJob?.cancel()
        val points = state.selectedPoints.toList()
        try {
            if (points.size >= 2) {
                val p1 = points[points.size - 2]
                val p2 = points[points.size - 1]
                if (!visualizeSteps)
                {
                    pathJob = viewModelScope.launch {
                        val path = withContext(Dispatchers.Default) {
                            lastPath = algorithm.findPath(p1.position.toPair(),p2.position.toPair()).map { p -> p.toOffset() }
                        }
                    }
                }
                else
                {
                    startPathfinding(p1.position.toPair(), p2.position.toPair())
                }
                isPathProcessing = true;
                pathJob?.invokeOnCompletion { cause ->
                    if (cause == null) {

                    }
                    isPathProcessing = false;
                }
            }
        }
        finally {
        }
    }

    fun cancelPathfinding(){
        pathJob?.cancel()
        currentStep = null;
        lastPath = emptyList()
    }

    fun startPathfinding(start: Pair<Int, Int>, end: Pair<Int, Int>) {
        pathJob?.cancel()

        pathJob = viewModelScope.launch {
            try {
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
            }
        }
    }

    fun deletePoint(index: Int) {
        if (index in state.selectedPoints.indices) {
            state.selectedPoints.removeAt(index)

            if (state.selectedPoints.size < 2) {
                lastPath = emptyList()
            } else {
                lastPath = emptyList()
            }
        }
    }

    fun clear() {
        state.clearPoints()
        currentStep = null;
        lastPath = emptyList()
    }

    fun Pair<Int, Int>.toOffset() = Offset(first.toFloat(), second.toFloat())
    fun Offset.toPair() = Pair<Int, Int>(x.toInt(), y.toInt())
}