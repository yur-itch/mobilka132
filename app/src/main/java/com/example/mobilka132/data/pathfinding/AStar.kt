package com.example.mobilka132.data.pathfinding

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*
import kotlin.collections.mutableMapOf
import kotlin.math.abs

class AStar {

    private val map : Array<Array<Int>>

    constructor(map : Array<Array<Int>>) {
        this.map = map
    }

    suspend fun findPath(s: Pair<Int, Int>, e: Pair<Int, Int>) : List<Pair<Int, Int>> {
        if (s.first >= map.size || s.second >= map.size || e.first >= map.size || e.second >= map.size) {
            println("Coordinate(s) out of array's bounds (${s.first}, ${s.second}) ($e.first, ${e.second})")
            return emptyList()
        }

        val allNodes = mutableMapOf<Pair<Int, Int>, Node>()
        val startNode : Node = getOrCreateNode(s, map, allNodes)
        val destinationNode : Node = getOrCreateNode(e, map, allNodes)
        val path : List<Node> = find(startNode, destinationNode, map, allNodes)
        val points: List<Pair<Int, Int>> = path.map { node ->
            Pair(node.x, node.y)
        }
        return points
    }

    private suspend fun find(start : Node, destination : Node, map : Array<Array<Int>>, allNodes :  MutableMap<Pair<Int, Int>, Node>) : List<Node> {
        var found = false
        val path : MutableList<Node> = mutableListOf()
        val closed : MutableSet<Node> = mutableSetOf()
        val minHeap = PriorityQueue<Node>()
        minHeap.add(start)
        while (minHeap.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val current = minHeap.poll()
            closed.add(current!!)
            if (current == destination) {
                found = true
                break
            }
            val neighbours = mutableListOf<Node>()
            for (x in -1 until 2){
                for(y in -1 until 2){
                    val i : Int = x + current.x
                    val j : Int = y + current.y
                    if (i >= 0 && i < map.size && j >= 0 && j < map[current.x].size && !(x == 0 && y == 0)){
                        neighbours.add(getOrCreateNode(Pair(x + current.x, y + current.y), map, allNodes))
                    }
                }
            }
            for (i in neighbours.indices){

                if (closed.contains(neighbours[i]) || !walkable(neighbours[i])){
                    continue
                }

                val newCost : Int = current.cost + getDistance(current, neighbours[i]) + neighbours[i].weight
                if (newCost < neighbours[i].cost || !minHeap.contains(neighbours[i])) {

                    neighbours[i].cost = newCost
                    neighbours[i].heuristicCost = getDistance(current, destination)
                    neighbours[i].parent = current
                    if (!minHeap.contains(neighbours[i])){
                        minHeap.add(neighbours[i])
                    }
                    else{
                        minHeap.remove(neighbours[i])
                        minHeap.add(neighbours[i])
                    }
                }
            }
        }
        if (found) {
            var current : Node = destination
            while (current != start) {
                path.add(current)
                current = current.parent!!
            }
            path.add(current)
        }
        return path.reversed()
    }

    private fun getDistance(start : Node, destination : Node) : Int {
        val dX = abs(start.x - destination.x)
        val dY = abs(start.y - destination.y)
        return if (dX >= dY) 14 * dY + 10 * (dX - dY) else 14 * dX + 10 * (dY - dX)
    }

    private fun walkable(node : Node) : Boolean {
        return node.weight < 5
    }

    private fun getOrCreateNode(coords : Pair<Int, Int>, map : Array<Array<Int>>, allNodes :  MutableMap<Pair<Int, Int>, Node>) : Node {
        return allNodes.getOrPut(coords) {
            Node(coords.first, coords.second, 5 - map[coords.first][coords.second] * 5)
        }
    }

    fun findPathAsync(s: Pair<Int, Int>, e: Pair<Int, Int>, delayMs: Long = 16L): Flow<AStarStep> = flow {
        val path : MutableList<Node> = mutableListOf()
        val allNodes = mutableMapOf<Pair<Int, Int>, Node>()
        val start : Node = getOrCreateNode(s, map, allNodes)
        val destination : Node = getOrCreateNode(e, map, allNodes)
        val closed : MutableSet<Node> = mutableSetOf()
        val minHeap = PriorityQueue<Node>()
        minHeap.add(start)

        while (minHeap.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val current = minHeap.poll()
            closed.add(current!!)

            emit(
                AStarStep(
                    Pair(current.x, current.y),
                    minHeap.map { item -> Pair(item.x, item.y) },
                    closed.map {
                            item -> Pair(item.x, item.y)
                    }.toList()
                )
            )

            if (current == destination) {
                var curr : Node = destination
                while (curr != start) {
                    path.add(curr)
                    curr = curr.parent!!
                }
                path.add(curr)

                emit(
                    AStarStep(
                        Pair(current.x, current.y),
                        minHeap.map { item -> Pair(item.x, item.y) },
                        closed.map {
                                item -> Pair(item.x, item.y)
                        }.toList(),
                        path = path.map { item -> Pair(item.x, item.y) }
                    )
                )
                return@flow
            }
            val neighbours = mutableListOf<Node>()
            for (x in -1 until 2){
                for(y in -1 until 2){
                    val i : Int = x + current.x
                    val j : Int = y + current.y
                    if (i >= 0 && i < map.size && j >= 0 && j < map[current.x].size && !(x == 0 && y == 0)){
                        neighbours.add(getOrCreateNode(Pair(x + current.x, y + current.y), map, allNodes))
                    }
                }
            }
            for (i in neighbours.indices){

                if (closed.contains(neighbours[i]) || !walkable(neighbours[i])){
                    continue
                }
                val newCost : Int = current.cost + getDistance(current, neighbours[i]) + neighbours[i].weight
                if (newCost < neighbours[i].cost || !minHeap.contains(neighbours[i])) {
                    println(getDistance(current, destination))
                    neighbours[i].cost = newCost
                    neighbours[i].heuristicCost = getDistance(current, destination)
                    neighbours[i].parent = current
                    if (!minHeap.contains(neighbours[i])){
                        minHeap.add(neighbours[i])
                    }
                    else{
                        minHeap.remove(neighbours[i])
                        minHeap.add(neighbours[i])
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

