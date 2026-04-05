package com.example.mobilka132.engine

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.example.mobilka132.data.pathfinding.AStar
import com.example.mobilka132.data.pathfinding.PathData
import com.example.mobilka132.model.AStarStep
import com.example.mobilka132.model.Path
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.Executors
import kotlin.coroutines.cancellation.CancellationException

class AStarEngine(private final val grid: Array<Array<Int>>) {
    val algorithm = AStar(grid)
}