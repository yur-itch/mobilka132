package com.example.mobilka132.model
import androidx.compose.ui.geometry.Offset

data class MapPoint(
    val id: Int,
    val position: Offset,
    val workingStart: Int = 0,
    val workingEnd: Int = 1440,
    val delay: Int = 0
)