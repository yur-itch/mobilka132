package com.example.mobilka132.data.pathfinding

data class AStarStepData(
    val current: Node,
    val openSet: List<Node>,
    val closedSet: List<Node>,
    val path: PathData? = null
)