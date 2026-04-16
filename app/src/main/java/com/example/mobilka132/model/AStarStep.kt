package com.example.mobilka132.model

import androidx.compose.ui.geometry.Offset

data class AStarStep(
    val current: Offset,
    val openSet: List<Offset>,
    val closedSet: List<Offset>,
    val path: Path? = null
)