package com.example.mobilka132

import android.graphics.BitmapFactory
import android.graphics.Color
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.core.graphics.get
import kotlinx.coroutines.*
import kotlin.math.min

class MapManager(val context: Context)  {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    lateinit var bitmap : Bitmap
    var loadedPoints: List<Offset> = emptyList()

    var activeJobs: MutableList<Job> = mutableListOf()

    var grid = Array(3000) { i ->
        Array(3000) { j -> 1 }
    }

    fun loadData() {
//        val job = scope.launch(Dispatchers.IO) {
        try {
            val cont = context.assets.open("test.png")
            bitmap = BitmapFactory.decodeStream(cont)
            cont.close()

            for(x in 0 until min(bitmap.width, grid.size)){
                for(y in 0 until min(bitmap.height, grid[0].size)){
                    val pixel = bitmap[x, y]
                    val blue = Color.blue(pixel)
                    grid[x][y] = if(blue > 127) 1 else 0
                }
            }
            Log.d("MAP_MANAGER", "Grid loaded")
        } catch (e: Exception) {
            Log.e("MAP_MANAGER", "Error loading bitmap", e)
        }
//        }
//        activeJobs.add(job)
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
        activeJobs.add(job)
    }

    fun cancelAll() {
        scope.cancel()
    }
}