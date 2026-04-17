package com.example.mobilka132.data.pathfinding

import androidx.compose.ui.geometry.Offset
import com.example.mobilka132.MapState
import com.example.mobilka132.model.AStarStep
import com.example.mobilka132.model.Path
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*
import kotlin.math.abs

class AStar(
    private val width: Int,
    private val height: Int,
    private val map: IntArray,
    private val state: MapState
) {
    private val maxNodeWeight = 5

    suspend fun findPath(s: Pair<Int, Int>, e: Pair<Int, Int>): List<Pair<Int, Int>> {
        if (s.first >= width || s.second >= height || e.first >= width || e.second >= height) {
            println("Coordinate(s) out of array's bounds (${s.first}, ${s.second}) (${e.first}, ${e.second})")
            return emptyList()
        }

        val allNodes = arrayOfNulls<Node>(width * height)
        val startNode = getOrCreateNode(s.first, s.second, allNodes)
        val destinationNode = getOrCreateNode(e.first, e.second, allNodes)

        val pathData = find(startNode, destinationNode, allNodes)
        return pathData.path.map { node -> Pair(node.x, node.y) }
    }

    suspend fun find(s: Pair<Int, Int>, e: Pair<Int, Int>): PathData {
        val allNodes = arrayOfNulls<Node>(width * height)
        val startNode = getOrCreateNode(s.first, s.second, allNodes)
        val destinationNode = getOrCreateNode(e.first, e.second, allNodes)
        return find(startNode, destinationNode, allNodes)
    }

    private suspend fun find(start: Node, destination: Node, allNodes: Array<Node?>): PathData {
        var found = false
        val closed = BooleanArray(width * height)
        val minHeap = PriorityQueue<Node>()

        start.cost = 0
        minHeap.add(start)

        while (minHeap.isNotEmpty()) {
            currentCoroutineContext().ensureActive()

            val current = minHeap.poll()!!
            val currentIdx = current.y * width + current.x

            if (closed[currentIdx]) continue
            closed[currentIdx] = true

            if (current == destination) {
                found = true
                break
            }

            for (x in -1..1) {
                for (y in -1..1) {
                    if (x == 0 && y == 0) continue
                    val i = x + current.x
                    val j = y + current.y

                    if (i in 0 until width && j in 0 until height) {
                        val node = getOrCreateNode(i, j, allNodes)
                        val nodeIdx = j * width + i

                        if (closed[nodeIdx] || !walkable(node)) continue

                        val newCost = current.cost + getDistance(current, node) + node.weight

                        if (newCost < node.cost) {
                            node.cost = newCost
                            node.heuristicCost = getDistance(node, destination)
                            node.parent = current
                            minHeap.add(node)
                        }
                    }
                }
            }
        }
        return if (found) retrace(start, destination) else PathData(emptyList(), 0f)
    }

    private fun getDistance(start: Node, destination: Node): Int {
        val dX = abs(start.x - destination.x)
        val dY = abs(start.y - destination.y)
        return if (dX >= dY) 14 * dY + 10 * (dX - dY)
        else 14 * dX + 10 * (dY - dX)
    }

    private fun walkable(node: Node): Boolean {
        return node.weight < maxNodeWeight
    }

    private fun getOrCreateNode(x: Int, y: Int, allNodes: Array<Node?>): Node {
        val index = y * width + x
        val existing = allNodes[index]
        if (existing != null) return existing
        val weight = maxNodeWeight * (1 - map[index])
        val newNode = Node(x, y, weight)
        allNodes[index] = newNode
        return newNode
    }

    private fun retrace(start: Node, destination: Node): PathData {
        val path = mutableListOf<Node>()
        var distance = 0f
        var current: Node? = destination
        while (current != null && current != start) {
            path.add(current)
            val parent = current.parent
            if (parent != null) {
                distance += getDistance(current, parent)
            }
            current = parent
        }
        if (current != null) path.add(current)

        val divisor = (10.0 / state.metersPerPixel).toFloat()
        return PathData(path.reversed(), distance / divisor)
    }

    fun findPathAsync(s: Pair<Int, Int>, e: Pair<Int, Int>, delayMs: Long = 16L): Flow<AStarStep> =
        flow {
            val allNodes = arrayOfNulls<Node>(width * height)

            val start = getOrCreateNode(s.first, s.second, allNodes)
            val destination = getOrCreateNode(e.first, e.second, allNodes)

            start.cost = 0
            val closedSet = mutableSetOf<Node>()
            val minHeap = PriorityQueue<Node>()
            minHeap.add(start)

            while (minHeap.isNotEmpty()) {
                currentCoroutineContext().ensureActive()
                val current = minHeap.poll()!!

                if (closedSet.contains(current)) continue
                closedSet.add(current)

                emit(
                    AStarStep(
                        current = Offset(current.x.toFloat(), current.y.toFloat()),
                        openSet = minHeap.map { Offset(it.x.toFloat(), it.y.toFloat()) },
                        closedSet = closedSet.map { Offset(it.x.toFloat(), it.y.toFloat()) }
                    )
                )

                if (current == destination) {
                    val pathData = retrace(start, destination)
                    emit(
                        AStarStep(
                            current = Offset(current.x.toFloat(), current.y.toFloat()),
                            openSet = minHeap.map { Offset(it.x.toFloat(), it.y.toFloat()) },
                            closedSet = closedSet.map { Offset(it.x.toFloat(), it.y.toFloat()) },
                            path = Path(
                                steps = pathData.path.map { node ->
                                    Offset(
                                        node.x.toFloat(),
                                        node.y.toFloat()
                                    )
                                },
                                distance = pathData.distance
                            )
                        )
                    )
                    return@flow
                }

                for (x in -1..1) {
                    for (y in -1..1) {
                        if (x == 0 && y == 0) continue
                        val i = x + current.x
                        val j = y + current.y

                        if (i in 0 until width && j in 0 until height) {
                            val node = getOrCreateNode(i, j, allNodes)
                            if (closedSet.contains(node) || !walkable(node)) continue

                            val newCost = current.cost + getDistance(current, node) + node.weight
                            if (newCost < node.cost) {
                                node.cost = newCost
                                node.heuristicCost = getDistance(node, destination)
                                node.parent = current
                                minHeap.add(node)
                            }
                        }
                    }
                }
                delay(delayMs)
            }
        }

    fun pathLength(path: List<Pair<Int, Int>>): Double {
        val totalInternal = (1 until path.size).sumOf { x ->
            val start = path[x - 1]
            val destination = path[x]
            val dX = abs(start.first - destination.first)
            val dY = abs(start.second - destination.second)
            (if (dX >= dY) 14 * dY + 10 * (dX - dY)
            else 14 * dX + 10 * (dY - dX)).toDouble()
        }
        val divisor = 10.0 / state.metersPerPixel
        return totalInternal / divisor
    }
}