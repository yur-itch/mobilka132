package com.example.mobilka132

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.content.Context

class MapManager(val context: Context) {
    var grid = Array(3000) { IntArray(3000) }
    fun loadData() {
        val cont = context.assets.open("test.png");

        println(cont);

        val bitmap = BitmapFactory.decodeStream(cont);
        println("Ширина: ${bitmap.width}, Высота: ${bitmap.height}")
        cont.close();

        for(x in 0 until bitmap.width){
            for(y in 0 until bitmap.height){
                val pixel = bitmap.getPixel(x,y);
                val red = Color.red(pixel);
                if(red > 200){
                    grid[x][y] = 1;
                }
                else{
                    grid[x][y] = 0;
                }
            }
        }
    }
}