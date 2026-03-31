package com.example.mobilka132.data.pathfinding

import androidx.compose.ui.geometry.Offset

data class Point(val x: Int, val y: Int) {
    fun toOffset() : Offset {
        return Offset(x.toFloat(), y.toFloat())
    }
}