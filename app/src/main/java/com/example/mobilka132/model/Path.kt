package com.example.mobilka132.model

import androidx.compose.ui.geometry.Offset

data class Path(
    val steps: List<Offset>,
    val distance: Float,
    val segments: List<PathSegment> = emptyList()
)

data class PathSegment(
    val start: Offset,
    val end: Offset,
    val isWalkable: Boolean
)
