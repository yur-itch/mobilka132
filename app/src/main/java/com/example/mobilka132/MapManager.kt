package com.example.mobilka132

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.content.Context
import com.example.mobilka132.data.pathfinding.Node
import androidx.core.graphics.get
import kotlin.math.min

class MapManager(val context: Context) {
    // 1 - white, 0 - black
    var grid = Array(3000) { i ->
        Array(3000) { j -> 0 }
    }

    fun loadData(){
        val cont = context.assets.open("test.png");

        println(cont);

        val bitmap = BitmapFactory.decodeStream(cont);
        println("Ширина: ${bitmap.width}, Высота: ${bitmap.height}")
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