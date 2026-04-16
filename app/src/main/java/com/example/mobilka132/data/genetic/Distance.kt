package com.example.mobilka132.data.genetic

import com.example.mobilka132.data.pathfinding.AStar
import kotlin.math.pow

interface Distance {
    operator fun get(i: Int, j: Int): Double
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

    override operator fun get(i: Int, j: Int): Double {
        return points[i].distanceTo(points[j])
    }

    override fun getPoint(i: Int): Point = points[i]
}

data class CachedPath(
    val path: List<Point>,
    val length: Double
)

private data class PointPair(val p1: Pair<Int, Int>, val p2: Pair<Int, Int>)

class WalkableDistance(
    private val algo: AStar
) : Distance {

    private var points: List<Point> = emptyList()
    override val size: Int get() = points.size

    private var lengthMatrix = DoubleArray(0)
    private var pathCache = arrayOfNulls<List<Point>>(0)

    // Persistent cache across setup() calls
    private val persistentCache = mutableMapOf<PointPair, CachedPath>()

    suspend fun setup(newPoints: List<Point>) {
        this.points = newPoints
        val n = newPoints.size
        lengthMatrix = DoubleArray(n * n) { -1.0 }
        pathCache = arrayOfNulls(n * n)

        for (i in 0 until n) {
            for (j in 0 until n) {
                if (i == j) {
                    lengthMatrix[i * n + j] = 0.0
                    continue
                }
                internalGetOrCompute(i, j)
            }
        }
    }

    override fun getPoint(i: Int): Point = points[i]

    override fun get(i: Int, j: Int): Double {
        val n = size
        if (n == 0 || i == j) return 0.0
        val dist = lengthMatrix[i * n + j]
        return if (dist < 0) 0.0 else dist
    }

    private suspend fun internalGetOrCompute(i: Int, j: Int): Double {
        val n = size
        val fastIdx = i * n + j

        val fastLen = lengthMatrix[fastIdx]
        if (fastLen >= 0) return fastLen

        val p1 = points[i].toPair()
        val p2 = points[j].toPair()

        val isReversed = p1.first > p2.first || (p1.first == p2.first && p1.second > p2.second)
        val key = if (isReversed) PointPair(p2, p1) else PointPair(p1, p2)

        val cached = persistentCache[key]
        val path: List<Point>
        val len: Double

        if (cached != null) {
            path = if (isReversed) cached.path.asReversed() else cached.path
            len = cached.length
        } else {
            val aStarPath = algo.findPath(p1, p2)
            len = algo.pathLength(aStarPath)
            val mappedPath = aStarPath.map { Point(it.first, it.second) }
            
            val pathToCache = if (isReversed) mappedPath.asReversed() else mappedPath
            persistentCache[key] = CachedPath(pathToCache, len)
            path = mappedPath
        }

        lengthMatrix[fastIdx] = len
        lengthMatrix[j * n + i] = len
        pathCache[fastIdx] = path
        return len
    }

    fun path(i: Int, j: Int): List<Point> {
        val n = size
        if (n == 0) return emptyList()
        if (i == j) return listOf(points[i])

        val idx = if (i < j) i * n + j else j * n + i
        val cached = pathCache[idx] ?: return emptyList()
        
        return if (i < j) cached else cached.asReversed()
    }
    
    fun clearPersistentCache() {
        persistentCache.clear()
    }
}
