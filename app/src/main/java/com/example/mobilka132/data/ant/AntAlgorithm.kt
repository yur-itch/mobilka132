package com.example.mobilka132.data.ant

import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import com.example.mobilka132.data.genetic.WalkableDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.random.Random

class AntAlgorithm(val walkableDistance: WalkableDistance) {
    val points = mutableStateListOf<Offset>()
    val bestPath = mutableStateListOf<Int>()
    val currentIteration = mutableIntStateOf(0)
    val bestDistance = mutableDoubleStateOf(Double.MAX_VALUE)

    fun setPoints(newPoints: List<Offset>) {
        points.clear()
        points.addAll(newPoints)
        bestPath.clear()
        currentIteration.intValue = 0
        bestDistance.doubleValue = Double.MAX_VALUE
    }

    fun generatePoints(w : Int, h : Int, n: Int) {
        points.clear()
        bestPath.clear()
        currentIteration.intValue = 0
        bestDistance.doubleValue = Double.MAX_VALUE
        repeat(n) {
            points.add(Offset(Random.nextFloat() * w, Random.nextFloat() * h))
        }
    }

    fun getFullBestPathSteps(): List<Offset> {
        val tour = bestPath.toList()
        if (tour.isEmpty() || bestDistance.doubleValue >= Double.MAX_VALUE) return emptyList()
        val fullSteps = mutableListOf<Offset>()
        for (i in 0 until tour.size) {
            val from = tour[i]
            val to = tour[(i + 1) % tour.size]
            
            val segment = walkableDistance.path(from, to).map { Offset(it.x.toFloat(), it.y.toFloat()) }
            if (segment.isEmpty() && from != to) return emptyList()
            
            fullSteps.addAll(segment)
        }
        return fullSteps
    }

    suspend fun solve(
        startNodeIndex: Int = -1,
        maxIterations: Int = 200,
        numAnts: Int = 20,
        alpha: Double = 1.0,
        beta: Double = 2.0,
        evaporation: Double = 0.5,
        Q: Double = 100.0,
        onIteration: (suspend (Int, List<Int>, Double) -> Unit)? = null
    ) {
        val n = points.size
        if (n < 2) return

        val dist = Array(n) { i ->
            DoubleArray(n) { j ->
                val d = walkableDistance[i, j]
                if (d <= 0 && i != j) Double.MAX_VALUE else d
            }
        }

        val pheromones = Array(n) { DoubleArray(n) { 1.0 } }
        var localBestPath = listOf<Int>()
        var localBestDist = Double.MAX_VALUE

        for (iteration in 0 until maxIterations) {
            val antPaths = mutableListOf<List<Int>>()
            val antDistances = DoubleArray(numAnts)

            for (ant in 0 until numAnts) {
                val path = mutableListOf<Int>()
                val visited = BooleanArray(n)
                
                var current = if (startNodeIndex in 0 until n) startNodeIndex else Random.nextInt(n)
                path.add(current)
                visited[current] = true

                while (path.size < n) {
                    val next = selectNextCity(current, visited, pheromones, dist, alpha, beta)
                    if (next == -1) break 
                    path.add(next)
                    visited[next] = true
                    current = next
                }

                if (path.size == n) {
                    antPaths.add(path)
                    val d = calculateTotalDist(path, dist)
                    antDistances[ant] = d

                    if (d < localBestDist) {
                        localBestDist = d
                        localBestPath = path.toList()

                        withContext(Dispatchers.Main) {
                            bestDistance.doubleValue = localBestDist
                            bestPath.clear()
                            bestPath.addAll(localBestPath)
                        }
                    }
                } else {
                    antDistances[ant] = Double.MAX_VALUE
                }
            }

            for (i in 0 until n) {
                for (j in 0 until n) {
                    pheromones[i][j] *= (1.0 - evaporation)
                }
            }

            for (ant in 0 until antPaths.size) {
                val d = antDistances[ant]
                if (d >= Double.MAX_VALUE) continue
                val p = antPaths[ant]
                for (i in 0 until n) {
                    val from = p[i]
                    val to = p[(i + 1) % n]
                    pheromones[from][to] += Q / d
                    pheromones[to][from] += Q / d
                }
            }

            withContext(Dispatchers.Main) {
                currentIteration.intValue = iteration + 1
            }

            if (localBestDist < Double.MAX_VALUE) {
                onIteration?.invoke(iteration + 1, localBestPath, localBestDist)
            }

            delay((100 / (iteration + 1)).toLong())
        }
    }

    private fun selectNextCity(
        current: Int,
        visited: BooleanArray,
        pheromones: Array<DoubleArray>,
        dist: Array<DoubleArray>,
        alpha: Double,
        beta: Double
    ): Int {
        val n = visited.size
        val probs = DoubleArray(n)
        var sum = 0.0

        for (i in 0 until n) {
            if (!visited[i]) {
                val d = dist[current][i]
                if (d >= Double.MAX_VALUE) continue
                val eta = if (d > 0) 1.0 / d else 1.0
                probs[i] = pheromones[current][i].pow(alpha) * eta.pow(beta)
                sum += probs[i]
            }
        }

        if (sum == 0.0) {
            return (0 until n).firstOrNull { !visited[it] && dist[current][it] < Double.MAX_VALUE } ?: -1
        }

        val r = Random.nextDouble() * sum
        var currentSum = 0.0
        for (i in 0 until n) {
            if (!visited[i] && dist[current][i] < Double.MAX_VALUE) {
                currentSum += probs[i]
                if (currentSum >= r) return i
            }
        }
        return (0 until n).firstOrNull { !visited[it] && dist[current][it] < Double.MAX_VALUE } ?: -1
    }

    private fun calculateTotalDist(path: List<Int>, dist: Array<DoubleArray>): Double {
        var d = 0.0
        for (i in 0 until path.size) {
            val from = path[i]
            val to = path[(i + 1) % path.size]
            val stepDist = dist[from][to]
            if (stepDist >= Double.MAX_VALUE) return Double.MAX_VALUE
            d += stepDist
        }
        return d
    }
}
