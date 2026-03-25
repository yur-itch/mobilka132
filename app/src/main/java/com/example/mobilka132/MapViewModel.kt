package com.example.mobilka132

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilka132.data.pathfinding.AStar
import com.example.mobilka132.data.pathfinding.Node
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapViewModel : ViewModel() {

    lateinit var algorithm: AStar
    val state = MapState()
    val overlay = MapOverlayRenderer(state)

    var lastPath by mutableStateOf<List<Offset>>(emptyList())

    fun init(grid: Array<Array<Int>>) {
        algorithm = AStar(grid)
    }

    fun onPointSelected(point: Offset, maskBitmap: Bitmap) {
        viewModelScope.launch {
            state.isProcessing = true
            state.addPoint(point, maskBitmap)
            val points = state.selectedPoints.toList()
            if (points.size >= 2) {
                val p1 = points[points.size - 2]
                val p2 = points[points.size - 1]

                val path = withContext(Dispatchers.Default) {
                    algorithm.findPath(
                        p1.x.toInt(), p1.y.toInt(),
                        p2.x.toInt(), p2.y.toInt()
                    )
                }
                lastPath = path.map { pair ->
                    Offset(pair.first.toFloat(), pair.second.toFloat())
                }
            }
            // finally
            state.isProcessing = false
        }
    }

    fun clear() {
        state.selectedPoints.clear()
        lastPath = emptyList()
    }
}