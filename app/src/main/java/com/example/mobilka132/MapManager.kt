package com.example.mobilka132

import android.graphics.BitmapFactory
import android.graphics.Color
import android.content.Context
import androidx.core.graphics.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class MapManager(val context: Context)  {
    // 1 - white, 0 - black
    var grid = Array(3000) { i ->
        Array(3000) { j -> 1 }
    }

    fun loadData() {
        val cont = context.assets.open("test.png");

        println(cont);

        val bitmap = BitmapFactory.decodeStream(cont);
        cont.close();

        for(x in 0 until min(bitmap.width, grid.size)){
            for(y in 0 until min(bitmap.height, grid[0].size)){
                val pixel = bitmap[x, y];
                val blue = Color.blue(pixel);
                if(blue > 127){
                    grid[x][y] = 1;
                }
                else{
                    grid[x][y] = 0;
                }
            }
        }
    }
}