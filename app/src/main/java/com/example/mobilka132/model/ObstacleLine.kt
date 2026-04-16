package com.example.mobilka132.model

import androidx.compose.ui.geometry.Offset

data class ObstacleLine(
    val id: Int,
    var start: Offset,
    var end: Offset
)