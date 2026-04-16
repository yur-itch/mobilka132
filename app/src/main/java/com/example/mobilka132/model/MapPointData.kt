package com.example.mobilka132.model

import androidx.compose.ui.geometry.Offset

data class MapPointData(
    val position: Offset,
    val start: Int,
    val end: Int,
    val delay: Int,
    val items: List<String> = emptyList()
)
