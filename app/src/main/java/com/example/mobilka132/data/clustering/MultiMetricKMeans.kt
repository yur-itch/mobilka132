package com.example.mobilka132.data.clustering

import androidx.compose.ui.geometry.Offset
import com.example.mobilka132.data.pathfinding.AStar
import com.example.mobilka132.model.ObstacleLine
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

enum class BonusViewMode {
    EUCLIDEAN,
    MANHATTAN,
    ASTAR,
    DIFFERENCES
}

data class PointMultiAssignment(
    val position: Offset,
    val euclideanCluster: Int,
    val manhattanCluster: Int,
    val astarCluster: Int
) {

    val hasConflict: Boolean
        get() = !(euclideanCluster == manhattanCluster && manhattanCluster == astarCluster)
}

data class MultiMetricResult(
    val assignments: List<PointMultiAssignment>,
    val euclideanMedoids: List<Offset>,
    val manhattanMedoids: List<Offset>,
    val astarMedoids: List<Offset>,
    val k: Int
)

object MultiMetricKMeans {

    suspend fun run(
        points: List<Offset>,
        k: Int,
        astar: AStar,
        obstacles: List<ObstacleLine> = emptyList(),
        onAStarProgress: suspend (Float) -> Unit
    ): MultiMetricResult = withContext(Dispatchers.Default) {

        val n = points.size
        val actualK = minOf(k, n)

        val astarMatrix = computeAStarMatrix(points, astar, onAStarProgress)

        val euclidMatrix    = computeEuclideanMatrix(points, obstacles, astarMatrix)
        val manhattanMatrix = computeManhattanMatrix(points, obstacles, astarMatrix)

        val (euclidAssign, euclidMedoidIdx)     = kMedoids(n, actualK, euclidMatrix)
        val (manhattanAssign, manhattanMedoidIdx) = kMedoids(n, actualK, manhattanMatrix)
        val (astarAssign, astarMedoidIdx)        = kMedoids(n, actualK, astarMatrix)

        val assignments = points.mapIndexed { i, pos ->
            PointMultiAssignment(
                position          = pos,
                euclideanCluster  = euclidAssign[i],
                manhattanCluster  = manhattanAssign[i],
                astarCluster      = astarAssign[i]
            )
        }

        val euclidMedoids    = euclidMedoidIdx.map { points[it] }
        val manhattanMedoids = manhattanMedoidIdx.map { points[it] }
        val astarMedoids     = astarMedoidIdx.map { points[it] }

        MultiMetricResult(
            assignments      = assignments,
            euclideanMedoids = euclidMedoids,
            manhattanMedoids = manhattanMedoids,
            astarMedoids     = astarMedoids,
            k                = actualK
        )
    }


    private fun segmentsIntersect(p1: Offset, p2: Offset, p3: Offset, p4: Offset): Boolean {
        val d1x = p2.x - p1.x; val d1y = p2.y - p1.y
        val d2x = p4.x - p3.x; val d2y = p4.y - p3.y
        val cross = d1x * d2y - d1y * d2x
        if (cross == 0f) return false
        val dx = p3.x - p1.x; val dy = p3.y - p1.y
        val t = (dx * d2y - dy * d2x) / cross
        val u = (dx * d1y - dy * d1x) / cross
        return t in 0f..1f && u in 0f..1f
    }

    private fun isBlocked(a: Offset, b: Offset, obstacles: List<ObstacleLine>): Boolean =
        obstacles.any { segmentsIntersect(a, b, it.start, it.end) }

    private fun computeEuclideanMatrix(points: List<Offset>, obstacles: List<ObstacleLine>, astarMatrix: DoubleArray): DoubleArray {
        val n = points.size
        val matrix = DoubleArray(n * n)
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val dist = if (obstacles.isNotEmpty() && isBlocked(points[i], points[j], obstacles)) {
                    astarMatrix[i * n + j]
                } else {
                    val dx = (points[i].x - points[j].x).toDouble()
                    val dy = (points[i].y - points[j].y).toDouble()
                    sqrt(dx * dx + dy * dy)
                }
                matrix[i * n + j] = dist
                matrix[j * n + i] = dist
            }
        }
        return matrix
    }

    private fun computeManhattanMatrix(points: List<Offset>, obstacles: List<ObstacleLine>, astarMatrix: DoubleArray): DoubleArray {
        val n = points.size
        val matrix = DoubleArray(n * n)
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val dist = if (obstacles.isNotEmpty() && isBlocked(points[i], points[j], obstacles)) {
                    astarMatrix[i * n + j]
                } else {
                    (abs(points[i].x - points[j].x) + abs(points[i].y - points[j].y)).toDouble()
                }
                matrix[i * n + j] = dist
                matrix[j * n + i] = dist
            }
        }
        return matrix
    }

    private suspend fun computeAStarMatrix(
        points: List<Offset>,
        astar: AStar,
        onProgress: suspend (Float) -> Unit
    ): DoubleArray = coroutineScope {

        val n      = points.size
        val matrix = DoubleArray(n * n)
        val pairs = (0 until n).flatMap { i -> (i + 1 until n).map { j -> i to j } }
        val total = pairs.size

        if (total == 0) return@coroutineScope matrix

        val completed = AtomicInteger(0)
        val reportStep = (total / 20).coerceAtLeast(1)

        val chunkSize = (total / 4).coerceAtLeast(1)
        pairs.chunked(chunkSize).map { chunk ->
            async {
                for ((i, j) in chunk) {
                    val from = Pair(points[i].x.toInt(), points[i].y.toInt())
                    val to   = Pair(points[j].x.toInt(), points[j].y.toInt())

                    val path = astar.findPath(from, to)

                    val dist = if (path.isNotEmpty()) {
                        astar.pathLength(path)
                    } else {
                        val dx = (points[i].x - points[j].x).toDouble()
                        val dy = (points[i].y - points[j].y).toDouble()
                        sqrt(dx * dx + dy * dy)
                    }

                    matrix[i * n + j] = dist
                    matrix[j * n + i] = dist

                    val done = completed.incrementAndGet()
                    if (done % reportStep == 0) {
                        withContext(Dispatchers.Main) {
                            onProgress(done.toFloat() / total)
                        }
                    }
                }
            }
        }.awaitAll()

        withContext(Dispatchers.Main) { onProgress(1f) }
        matrix
    }

    private fun kMedoids(
        n: Int,
        k: Int,
        distMatrix: DoubleArray,
        maxIterations: Int = 50
    ): Pair<IntArray, List<Int>> {

        val medoids = mutableListOf<Int>()

        medoids.add(Random.nextInt(n))

        repeat(k - 1) {

            val weights = DoubleArray(n) { i ->
                val minDist = medoids.minOf { m -> distMatrix[i * n + m] }
                minDist * minDist
            }

            val totalWeight = weights.sum()
            if (totalWeight <= 0.0) {
                medoids.add(Random.nextInt(n))
                return@repeat
            }

            var roulette = Random.nextDouble() * totalWeight
            var selected = n - 1
            for (i in weights.indices) {
                roulette -= weights[i]
                if (roulette <= 0.0) { selected = i; break }
            }
            medoids.add(selected)
        }

        var assignments = IntArray(n) { 0 }

        repeat(maxIterations) {

            val newAssignments = IntArray(n) { i ->
                medoids.indices.minByOrNull { m ->
                    distMatrix[i * n + medoids[m]]
                } ?: 0
            }

            if (newAssignments.contentEquals(assignments)) return@repeat
            assignments = newAssignments

            for (clusterIdx in 0 until k) {

                val clusterPoints = (0 until n).filter { assignments[it] == clusterIdx }
                if (clusterPoints.isEmpty()) continue

                val bestMedoid = clusterPoints.minByOrNull { candidate ->
                    clusterPoints.sumOf { other ->
                        distMatrix[candidate * n + other]
                    }
                } ?: continue

                medoids[clusterIdx] = bestMedoid
            }
        }

        val finalAssignments = IntArray(n) { i ->
            medoids.indices.minByOrNull { m ->
                distMatrix[i * n + medoids[m]]
            } ?: 0
        }

        return Pair(finalAssignments, medoids.toList())
    }
}
