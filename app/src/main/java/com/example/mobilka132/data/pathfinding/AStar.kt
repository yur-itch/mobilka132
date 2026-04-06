package com.example.mobilka132.data.pathfinding

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

        val path: List<Node> = find(startNode, destinationNode, map, allNodes)

        return path.map { node -> Pair(node.x, node.y) }
    }

    private suspend fun find(
        start: Node,
        destination: Node,
        map: Array<Array<Int>>,
        allNodes: Array<Node?>
    ): List<Node> {
        var found = false
        val path: MutableList<Node> = mutableListOf()
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
                            elem.heuristicCost = getDistance(current, destination)
                            elem.parent = current
                            minHeap.add(elem)
                        }
                    }
                }
            }
        }

        if (found) {
            var curr: Node? = destination
            while (curr != null && curr != start) {
                path.add(curr)
                curr = curr.parent
            }
            if (curr != null) path.add(curr)
        }
        return path.reversed()
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

    fun findPathAsync(s: Pair<Int, Int>, e: Pair<Int, Int>, delayMs: Long = 16L): Flow<AStarStep> = flow {
        val path: MutableList<Node> = mutableListOf()
        val width = map.size
        val height = map[0].size
        val allNodes = arrayOfNulls<Node>(width * height)

        val start: Node = getOrCreateNode(s.first, s.second, map, allNodes)
        val destination: Node = getOrCreateNode(e.first, e.second, map, allNodes)

        start.cost = 0

        val closed: MutableSet<Node> = mutableSetOf()
        val minHeap = PriorityQueue<Node>()

        minHeap.add(start)

        while (minHeap.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val current = minHeap.poll() ?: break
            if (closed.contains(current)) continue
            closed.add(current)

            emit(
                AStarStep(
                    Pair(current.x, current.y),
                    minHeap.map { item -> Pair(item.x, item.y) },
                    closed.map { item -> Pair(item.x, item.y) }.toList()
                )
            )

            if (current == destination) {
                var curr: Node? = destination
                while (curr != null && curr != start) {
                    path.add(curr)
                    curr = curr.parent
                }
                if (curr != null) path.add(curr)

                emit(
                    AStarStep(
                        Pair(current.x, current.y),
                        minHeap.map { item -> Pair(item.x, item.y) },
                        closed.map { item -> Pair(item.x, item.y) }.toList(),
                        path = path.map { item -> Pair(item.x, item.y) }
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

                        if (closed.contains(elem) || !walkable(elem)) {
                            continue
                        }

                        val newCost: Int = current.cost + getDistance(current, elem) + elem.weight

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