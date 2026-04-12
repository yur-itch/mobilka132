package com.example.mobilka132

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilka132.data.pathfinding.AStar
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

    fun onPointSelected(screenOffset: Offset, roadMask: Bitmap, buildingsMask: Bitmap) {
        viewModelScope.launch {
            try {
                val contentPoint = state.screenToContent(screenOffset)
                state.handleMapClick(contentPoint, roadMask, buildingsMask)

                val points = state.selectedPoints
                if (points.size >= 2) {
                    val p1 = points[points.size - 2]
                    val p2 = points[points.size - 1]

                    val path = withContext(Dispatchers.Default) {
                        algorithm.findPath(
                            p1.position.x.toInt(), p1.position.y.toInt(),
                            p2.position.x.toInt(), p2.position.y.toInt()
                        )
                    }

                    lastPath = path.map { pair ->
                        Offset(pair.first.toFloat(), pair.second.toFloat())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePoint(index: Int) {
        if (index in state.selectedPoints.indices) {
            state.selectedPoints.removeAt(index)
            lastPath = emptyList()
        }
    }

    fun clear() {
        state.clearPoints()
        lastPath = emptyList()
    }
}
