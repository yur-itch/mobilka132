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

    private val coordinateCache = mutableMapOf<Pair<Point, Point>, CachedPath>()

    fun setPoints(newPoints: List<Point>) {
        this.points = newPoints
    }

    override fun getPoint(i: Int): Point = points[i]

    private fun key(p1: Point, p2: Point): Pair<Point, Point> {
        return if (p1.x < p2.x || (p1.x == p2.x && p1.y < p2.y)) p1 to p2 else p2 to p1
    }

    private suspend fun getCached(p1: Point, p2: Point): CachedPath {
        val k = key(p1, p2)
        coordinateCache[k]?.let { return it }

        val path = algo.findPath(k.first.toPair(), k.second.toPair())
        val length = algo.pathLength(path)
        val cached = CachedPath(
            path = path.map { Point(it.first, it.second) }, 
            length = length
        )

        coordinateCache[k] = cached
        return cached
    }

    override suspend operator fun get(i: Int, j: Int): Double =
        getCached(points[i], points[j]).length

    suspend fun path(i: Int, j: Int): List<Point> {
        val p1 = points[i]
        val p2 = points[j]
        val res = getCached(p1, p2)
        val k = key(p1, p2)
        return if (p1 == k.first) res.path else res.path.reversed()
    }
}