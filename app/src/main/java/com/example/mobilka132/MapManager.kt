package com.example.mobilka132

import android.graphics.BitmapFactory
import android.graphics.Color
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.example.mobilka132.model.ObstacleLine
import kotlinx.coroutines.*
import kotlin.math.abs

class MapManager(val context: Context)  {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    lateinit var bitmap : Bitmap
    var loadedPoints: List<Offset> = emptyList()

    var width: Int = 0
    var height: Int = 0
    var grid = IntArray(0)
    private var baseGrid = IntArray(0)

    fun loadData() : Deferred<Int> {
        val deferred = scope.async(Dispatchers.IO)
        {
            try {
                val cont = context.assets.open("map750.png")
                bitmap = BitmapFactory.decodeStream(cont)
                cont.close()
                width = bitmap.width
                height = bitmap.height
                baseGrid = IntArray(width * height)
                bitmap.getPixels(baseGrid, 0, width, 0, 0, width, height)
                for (i in 0 until width * height) {
                    if (Color.blue(baseGrid[i]) > 127) {
                        baseGrid[i] = 1
                    } else {
                        baseGrid[i] = 0
                    }
                }
                grid = baseGrid.copyOf()
                Log.d("MAP_MANAGER", "Grid loaded")
            } catch (e: Exception) {
                Log.e("MAP_MANAGER", "Error loading bitmap", e)
            }
        }
        return deferred
    }

    fun loadPointsFromAssets() {
        val job = scope.launch(Dispatchers.IO) {
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
                Log.d("GA_POINTS", "Successfully loaded ${points.size} points")
            } catch (e: Exception) {
                Log.e("GA_POINTS", "Error loading points", e)
            }
        }
    }

        fun updateObstacles(lines: List<ObstacleLine>) {
            val newGrid = baseGrid.copyOf()
            lines.forEach { line ->
                drawLineOnGrid(newGrid, line.start, line.end, 0)
            }
            newGrid.copyInto(grid)
        }

    private fun drawLineOnGrid(grid: IntArray, start: Offset, end: Offset, value: Int, thickness: Int = 2) {
        var x0 = start.x.toInt().coerceIn(0, width - 1)
        var y0 = start.y.toInt().coerceIn(0, height - 1)
        val x1 = end.x.toInt().coerceIn(0, width - 1)
        val y1 = end.y.toInt().coerceIn(0, height - 1)

        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy

        val offset = thickness / 2

        while (true) {
            for (ix in 0 until thickness) {
                for (iy in 0 until thickness) {
                    val px = x0 + ix - offset
                    val py = y0 + iy - offset

                    if (px in 0 until width && py in 0 until height) {
                        grid[py * width + px] = value
                    }
                }
            }

            if (x0 == x1 && y0 == y1) break
            val e2 = 2 * err
            if (e2 > -dy) { err -= dy; x0 += sx }
            if (e2 < dx) { err += dx; y0 += sy }
        }
    }

    fun cancelAll() {
        scope.cancel()
    }
}