package com.example.mobilka132.data.genetic

import com.example.mobilka132.data.pathfinding.AStar
import kotlin.math.pow

interface Distance {
    suspend operator fun get(i: Int, j: Int): Double
    val size: Int
    fun getPoint(i: Int): Point
}

data class Point(
    val x: Int,
    val y: Int,
    val workingStart: Int = 0,
    val workingEnd: Int = 1440,
    val delay: Int = 0
) {
    fun distanceTo(other: Point): Double {
        return ((x - other.x).toDouble().pow(2) + (y - other.y).toDouble().pow(2)).pow(0.5)
    }

    fun toPair(): Pair<Int, Int> {
        return Pair(this.x, this.y)
    }
}

class EucledianDistance(private val points: List<Point>) : Distance {
    override val size: Int = points.size

    override suspend operator fun get(i: Int, j: Int): Double {
        return points[i].distanceTo(points[j])
    }

    override fun getPoint(i: Int): Point = points[i]
}

data class CachedPath(
    val path: List<Point>,
    val length: Double
)

class WalkableDistance(
    private val algo: AStar
) : Distance {

    private var points: List<Point> = emptyList()
    override val size: Int get() = points.size

    private var lengthMatrix = DoubleArray(0)
    private var pathCache = arrayOfNulls<List<Point>>(0)

    suspend fun setup(newPoints: List<Point>) {
        this.points = newPoints
        val n = newPoints.size
        lengthMatrix = DoubleArray(n * n)
        pathCache = arrayOfNulls(n * n)

        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val idx = i * n + j
                val p1 = points[i].toPair()
                val p2 = points[j].toPair()

                val aStarPath = algo.findPath(p1, p2)
                val len = algo.pathLength(aStarPath)
                val mappedPath = aStarPath.map { Point(it.first, it.second) }

                lengthMatrix[idx] = len
                lengthMatrix[j * n + i] = len
                pathCache[idx] = mappedPath
            }
        }
    }

    override fun getPoint(i: Int): Point = points[i]

    override suspend fun get(i: Int, j: Int): Double {
        return lengthMatrix[i * size + j]
    }

    fun path(i: Int, j: Int): List<Point> {
        val n = size
        return if (i < j) {
            pathCache[i * n + j] ?: emptyList()
        } else {
            pathCache[j * n + i]?.reversed() ?: emptyList()
        }
    }
}