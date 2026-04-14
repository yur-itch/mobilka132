package com.example.mobilka132.data.pathfinding

import com.example.mobilka132.model.AStarStep
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

class AStar {

    private var width : Int
    private var height : Int
    private var map: IntArray
    private val maxNodeWeight = 5;

    constructor(width : Int, height : Int, map: IntArray) {
        this.width = width
        this.height = height
        this.map = map
    }

    suspend fun findPath(s: Pair<Int, Int>, e: Pair<Int, Int>): List<Pair<Int, Int>> {
        if (s.first >= map.size || s.second >= map.size || e.first >= map.size || e.second >= map.size) {
            println("Coordinate(s) out of array's bounds (${s.first}, ${s.second}) ($e.first, ${e.second})")
            return emptyList()
        }

        val allNodes = arrayOfNulls<Node>(width * height)
        val startNode: Node = getOrCreateNode(s.first, s.second, map, allNodes)
        val destinationNode: Node = getOrCreateNode(e.first, e.second, map, allNodes)

        val pathData = find(startNode, destinationNode, allNodes)
        return pathData.path.map { node -> Pair(node.x, node.y) }
    }

    suspend fun find(s: Pair<Int, Int>, e: Pair<Int, Int>): PathData {
        val allNodes = arrayOfNulls<Node>(width * height)
        val startNode = getOrCreateNode(s.first, s.second, map, allNodes)
        val destinationNode = getOrCreateNode(e.first, e.second, map, allNodes)
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

                    if (i in 0 until width && j in 0 until height) {
                        val node = getOrCreateNode(i, j, map, allNodes)
                        val nodeIndex = i * height + j

                        if (closed[nodeIndex] || !walkable(node)) continue

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
        if (found) {
            return retrace(start, destination)
        }
        return PathData(emptyList(), 0f)
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

    private fun getOrCreateNode(x: Int, y: Int, map: IntArray, allNodes: Array<Node?>): Node {
        val index = y * width + x
        val existing = allNodes[index]
        if (existing != null) return existing
        val newNode = Node(x, y, maxNodeWeight * (1 - map[index]))
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
        return PathData(path.reversed(), distance / 20f)
    }

    fun findPathAsync(s: Pair<Int, Int>, e: Pair<Int, Int>, delayMs: Long = 16L): Flow<AStarStepData> = flow {
        val allNodes = arrayOfNulls<Node>(width * height)

        val start: Node = getOrCreateNode(s.first, s.second, map, allNodes)
        val destination: Node = getOrCreateNode(e.first, e.second, map, allNodes)

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
                AStarStepData(
                    current,
                    minHeap.toList(),
                    closedSet.toList()
                )
            )

            if (current == destination) {
                val pathData = retrace(start, destination)
                emit(
                    AStarStepData(
                        current,
                        minHeap.toList(),
                        closedSet.toList(),
                        pathData
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
                        val node = getOrCreateNode(i, j, map, allNodes)
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
}