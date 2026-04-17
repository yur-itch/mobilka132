package com.example.mobilka132.data.clustering

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt
import kotlin.random.Random

data class ClusterPoint(
    val position: Offset,
    val clusterIndex: Int
)

data class ClusteringResult(
    val points: List<ClusterPoint>,
    val centroids: List<Offset>,
    val k: Int
)


object KMeans {

    fun cluster(
        points: List<Offset>,
        k: Int,
        maxIterations: Int = 100
    ): ClusteringResult {
        if (points.isEmpty()) return ClusteringResult(emptyList(), emptyList(), k)

        val actualK = minOf(k, points.size)

        var centroids = initCentroidsKMeansPlusPlus(points, actualK)

        var assignments = IntArray(points.size) { 0 }

        repeat(maxIterations) {

            val newAssignments = assignPoints(points, centroids)

            if (newAssignments.contentEquals(assignments)) {
                return@repeat
            }
            assignments = newAssignments

            centroids = recalculateCentroids(points, assignments, actualK, centroids)
        }

        val clusterPoints = points.mapIndexed { i, pos ->
            ClusterPoint(position = pos, clusterIndex = assignments[i])
        }

        return ClusteringResult(points = clusterPoints, centroids = centroids, k = actualK)
    }

    private fun initCentroidsKMeansPlusPlus(points: List<Offset>, k: Int): List<Offset> {
        val centroids = mutableListOf<Offset>()

        centroids.add(points[Random.nextInt(points.size)])

        repeat(k - 1) {
            val weights = points.map { point ->
                val minDist = centroids.minOf { c -> euclideanDistance(point, c) }
                minDist * minDist
            }

            val totalWeight = weights.sum()
            var roulette = Random.nextDouble() * totalWeight
            var selected = points.last()
            for (i in weights.indices) {
                roulette -= weights[i]
                if (roulette <= 0.0) {
                    selected = points[i]
                    break
                }
            }
            centroids.add(selected)
        }

        return centroids
    }

    private fun assignPoints(points: List<Offset>, centroids: List<Offset>): IntArray {
        return IntArray(points.size) { i ->
            centroids.indices.minByOrNull { j ->
                euclideanDistance(points[i], centroids[j])
            } ?: 0
        }
    }

    private fun recalculateCentroids(
        points: List<Offset>,
        assignments: IntArray,
        k: Int,
        oldCentroids: List<Offset>
    ): List<Offset> {
        return (0 until k).map { clusterIdx ->
            val clusterPoints = points.filterIndexed { i, _ -> assignments[i] == clusterIdx }

            if (clusterPoints.isEmpty()) {
                oldCentroids[clusterIdx]
            } else {
                Offset(
                    x = clusterPoints.sumOf { it.x.toDouble() }.toFloat() / clusterPoints.size,
                    y = clusterPoints.sumOf { it.y.toDouble() }.toFloat() / clusterPoints.size
                )
            }
        }
    }

    private fun euclideanDistance(a: Offset, b: Offset): Double {
        val dx = (a.x - b.x).toDouble()
        val dy = (a.y - b.y).toDouble()
        return sqrt(dx * dx + dy * dy)
    }
}
