package com.example.mobilka132.data.pathfinding

import androidx.compose.ui.geometry.Offset
import java.util.*
import kotlin.math.abs


class AStar {

    val grid : Array<Array<Node>>

    var lastPath : List<Node> = emptyList()

    constructor(map : Array<Array<Int>>) {
        grid = Array(map.size) { i ->
            Array(map[i].size) { j ->
                Node(i.toShort(), j.toShort(), (1 - map[i][j]).toShort())
            }
        }
        println(grid.size)
    }

    public fun findPath(x1 : Int, y1 : Int, x2 : Int, y2 : Int) {
        val startNode : Node = grid[x1][y1]
        val destinationNode : Node = grid[x2][y2]

        lastPath = find(startNode, destinationNode)
    }

    public fun find(start : Node, destination : Node) : List<Node> {
        var found = false
        val path : MutableList<Node> = mutableListOf()
        if (start.penalty == 0.toShort() && destination.penalty == 0.toShort()) {
            return path;
        }
        val closed : MutableSet<Node> = mutableSetOf()
        val minHeap = PriorityQueue<Node>()
        minHeap.add(start)

        while (minHeap.isNotEmpty()) {

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
                    if (i >= 0 && i < grid.size && j >= 0 && j < grid[current.x.toInt()].size && !(x == 0 && y == 0)){
                        neighbours.add(grid[i][j])
                    }
                }
            }
            for (i in neighbours.indices){

                if (closed.contains(neighbours[i])){
                    continue
                }

                val newCost : Int = current.cost.toInt() + getDistance(current, neighbours[i]) + neighbours[i].penalty
                if (newCost < neighbours[i].cost) {
                    neighbours[i].cost = newCost.toShort()
                    neighbours[i].heuristicCost = getDistance(current, neighbours[i]).toShort()
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
        println(path.size)
        return path.reversed()
    }

    private fun getDistance(start : Node, destination : Node) : Int {
        val dX = abs(start.x - destination.x)
        val dY = abs(start.y - destination.y)
        return if (dX >= dY) 14 * dY + 10 * (dX - dY) else 14 * dX + 10 * (dY - dX)
    }
}

