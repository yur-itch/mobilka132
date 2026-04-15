package com.example.mobilka132.data.ant

import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import com.example.mobilka132.data.pathfinding.AStar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.random.Random

class AntColony(val pathfinder: AStar) {
    val points = mutableStateListOf<Offset>()
    val bestPath = mutableStateListOf<Int>()
    val currentIteration = mutableIntStateOf(0)
    val bestDistance = mutableDoubleStateOf(Double.MAX_VALUE)

    private val distanceCache = mutableMapOf<Pair<Int, Int>, Double>()

    fun generatePoints(n: Int) {
        points.clear()
        bestPath.clear()
        distanceCache.clear()
        currentIteration.intValue = 0
        bestDistance.doubleValue = Double.MAX_VALUE
        repeat(n) {
            points.add(Offset(Random.nextFloat() * 3000, Random.nextFloat() * 3000))
        }
    }

    private suspend fun getDistance(i: Int, j: Int): Double {
        if (i == j) return 0.0
        val key = if (i < j) i to j else j to i
        distanceCache[key]?.let { return it }

        val p1 = points[i]
        val p2 = points[j]
        
        val path = pathfinder.findPath(p1.x.toInt() to p1.y.toInt(), p2.x.toInt() to p2.y.toInt())
        val length = pathfinder.pathLength(path)
        
        distanceCache[key] = length
        return length
    }

    suspend fun solve(
        maxIterations: Int = 200,
        numAnts: Int = 20,
        alpha: Double = 1.0,
        beta: Double = 2.0,
        evaporation: Double = 0.5,
        Q: Double = 100.0
    ) {
        val n = points.size
        if (n < 2) return

        val dist = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val d = getDistance(i, j)
                dist[i][j] = d
                dist[j][i] = d
            }
        }

        val pheromones = Array(n) { DoubleArray(n) { 1.0 } }
        var localBestPath = listOf<Int>()
        var localBestDist = Double.MAX_VALUE

        withContext(Dispatchers.Default) {
            for (iteration in 0 until maxIterations) {
                val antPaths = mutableListOf<List<Int>>()
                val antDistances = DoubleArray(numAnts)

                for (ant in 0 until numAnts) {
                    val path = mutableListOf<Int>()
                    val visited = BooleanArray(n)
                    var current = Random.nextInt(n)
                    path.add(current)
                    visited[current] = true

                    while (path.size < n) {
                        val next = selectNextCity(current, visited, pheromones, dist, alpha, beta)
                        path.add(next)
                        visited[next] = true
                        current = next
                    }

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
                }

                for (i in 0 until n) {
                    for (j in 0 until n) {
                        pheromones[i][j] *= (1.0 - evaporation)
                    }
                }

                for (ant in 0 until numAnts) {
                    val d = antDistances[ant]
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

                delay(5)
            }
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
                val eta = if (d > 0) 1.0 / d else 1.0
                probs[i] = pheromones[current][i].pow(alpha) * eta.pow(beta)
                sum += probs[i]
            }
        }

        if (sum == 0.0) return (0 until n).first { !visited[it] }

        val r = Random.nextDouble() * sum
        var currentSum = 0.0
        for (i in 0 until n) {
            if (!visited[i]) {
                currentSum += probs[i]
                if (currentSum >= r) return i
            }
        }
        return (0 until n).first { !visited[it] }
    }

    private fun calculateTotalDist(path: List<Int>, dist: Array<DoubleArray>): Double {
        var d = 0.0
        for (i in 0 until path.size) {
            d += dist[path[i]][path[(i + 1) % path.size]]
        }
        return d
    }
}
