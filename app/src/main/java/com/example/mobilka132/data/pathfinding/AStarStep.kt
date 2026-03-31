package com.example.mobilka132.data.pathfinding

data class AStarStep(
    val current: Pair<Int, Int>,
    val openSet: List<Pair<Int, Int>>,
    val closedSet: List<Pair<Int, Int>>,
    val path: List<Pair<Int, Int>>? = null
)