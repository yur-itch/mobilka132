package com.example.mobilka132.model

data class AStarStep(
    val current: Pair<Int, Int>,
    val openSet: List<Pair<Int, Int>>,
    val closedSet: List<Pair<Int, Int>>,
    val path: Path? = null
)