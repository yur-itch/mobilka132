package com.example.mobilka132

import android.graphics.BitmapFactory
import android.graphics.Color
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.*
import kotlin.math.min

class MapManager(val context: Context)  {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    lateinit var bitmap : Bitmap
    var loadedPoints: List<Offset> = emptyList()

    var width: Int = 3000
    var height: Int = 3000
    var grid = IntArray(0)

    fun loadData() : Deferred<Int> {
        val deferred = scope.async(Dispatchers.IO)
        {
            try {
                val cont = context.assets.open("test.png")
                bitmap = BitmapFactory.decodeStream(cont)
                cont.close()
                width = bitmap.width
                height = bitmap.height
                grid = IntArray(width * height)
                bitmap.getPixels(grid, 0, width, 0, 0, width, height)
                for (i in 0 until width * height) {
                    if (Color.blue(grid[i]) > 127) {
                        grid[i] = 1
                    } else {
                        grid[i] = 0
                    }
                }
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

    fun cancelAll() {
        scope.cancel()
    }
}