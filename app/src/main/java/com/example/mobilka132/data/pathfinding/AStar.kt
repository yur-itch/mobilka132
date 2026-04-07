package com.example.mobilka132.data.pathfinding

import com.example.mobilka132.model.AStarStep
import com.example.mobilka132.model.PathData
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*
import kotlin.math.abs

class AStar {

    private val map: Array<Array<Int>>

    constructor(map: Array<Array<Int>>) {
        this.map = map
    }

    suspend fun findPath(s: Pair<Int, Int>, e: Pair<Int, Int>): List<Pair<Int, Int>> {
        if (s.first >= map.size || s.second >= map.size || e.first >= map.size || e.second >= map.size) {
            println("Coordinate(s) out of array's bounds (${s.first}, ${s.second}) ($e.first, ${e.second})")
            return emptyList()
        }

        val allNodes = arrayOfNulls<Node>(map.size * map[0].size)
        val startNode: Node = getOrCreateNode(s.first, s.second, map, allNodes)
        val destinationNode: Node = getOrCreateNode(e.first, e.second, map, allNodes)

        val pathData = find(startNode, destinationNode, allNodes)
        return pathData.steps.map { node -> Pair(node.x, node.y) }
    }

    suspend fun find(s: Pair<Int, Int>, e: Pair<Int, Int>): PathData {
        val allNodes = arrayOfNulls<Node>(map.size * map[0].size)
        val startNode = getOrCreateNode(s.first, s.second, map, allNodes)
        val destinationNode = getOrCreateNode(e.first, e.second, map, allNodes)
        return find(startNode, destinationNode, allNodes)
    }

    private suspend fun find(
        start: Node,
        destination: Node,
        allNodes: Array<Node?>
    ): PathData {
        var found = false
        val width = map.size
        val height = map[0].size
        val closed = BooleanArray(width * height)
        val minHeap = PriorityQueue<Node>()

        start.cost = 0
        minHeap.add(start)

        while (minHeap.isNotEmpty()) {
            currentCoroutineContext().ensureActive()

            val current = minHeap.poll() ?: break
            val currentIdx = current.x * height + current.y

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

                    if (i >= 0 && i < width && j >= 0 && j < height) {
                        val elem = getOrCreateNode(i, j, map, allNodes)
                        val elemIdx = i * height + j

                        if (closed[elemIdx] || !walkable(elem)) continue

                        val newCost = current.cost + getDistance(current, elem) + elem.weight

                        if (newCost < elem.cost) {
                            elem.cost = newCost
                            elem.heuristicCost = getDistance(elem, destination)
                            elem.parent = current
                            minHeap.add(elem)
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
        return node.weight < 5
    }

    private fun getOrCreateNode(
        x: Int, y: Int,
        map: Array<Array<Int>>,
        allNodes: Array<Node?>
    ): Node {
        val index = (x * map[0].size) + y
        val existing = allNodes[index]
        if (existing != null) return existing
        val newNode = Node(x, y, 5 - map[x][y] * 5)
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
        return PathData(path.reversed(), distance / 10f)
    }

    fun findPathAsync(s: Pair<Int, Int>, e: Pair<Int, Int>, delayMs: Long = 16L): Flow<AStarStep> = flow {
        val width = map.size
        val height = map[0].size
        val allNodes = arrayOfNulls<Node>(width * height)

        val start: Node = getOrCreateNode(s.first, s.second, map, allNodes)
        val destination: Node = getOrCreateNode(e.first, e.second, map, allNodes)

        start.cost = 0
        val closedSet = mutableSetOf<Node>()
        val minHeap = PriorityQueue<Node>()
        minHeap.add(start)

        while (minHeap.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val current = minHeap.poll() ?: break

            if (closedSet.contains(current)) continue
            closedSet.add(current)

            emit(
                AStarStep(
                    Pair(current.x, current.y),
                    minHeap.map { Pair(it.x, it.y) },
                    closedSet.map { Pair(it.x, it.y) }
                )
            )

            if (current == destination) {
                val pathData = retrace(start, destination)
                emit(
                    AStarStep(
                        Pair(current.x, current.y),
                        minHeap.map { Pair(it.x, it.y) },
                        closedSet.map { Pair(it.x, it.y) },
                        path = pathData.steps.map { Pair(it.x, it.y) }
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
                        val elem = getOrCreateNode(i, j, map, allNodes)
                        if (closedSet.contains(elem) || !walkable(elem)) continue

                        val newCost = current.cost + getDistance(current, elem) + elem.weight
                        if (newCost < elem.cost) {
                            elem.cost = newCost
                            elem.heuristicCost = getDistance(elem, destination)
                            elem.parent = current
                            minHeap.add(elem)
                        }
                    }
                }
            }
            delay(delayMs)
        }
    }

    fun pathLength(path: List<Pair<Int, Int>>): Double {
        return (1 until path.size).sumOf { x ->
            val start = path[x - 1]
            val destination = path[x]
            val dX = abs(start.first - destination.first)
            val dY = abs(start.second - destination.second)
            (if (dX >= dY) 14 * dY + 10 * (dX - dY)
            else 14 * dX + 10 * (dY - dX)).toDouble()
        }
    }
}