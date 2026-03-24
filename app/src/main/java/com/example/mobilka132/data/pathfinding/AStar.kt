package com.example.mobilka132.data.pathfinding

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.Paragraph
import java.util.*
import kotlin.collections.mutableMapOf
import kotlin.math.abs


class AStar {

    val map : Array<Array<Int>>
    var lastPath : List<Node> = emptyList()

    constructor(map : Array<Array<Int>>) {
        this.map = map
    }

    public fun findPath(x1 : Int, y1 : Int, x2 : Int, y2 : Int) {
        if (x1 >= map.size || x2 >= map.size || y1 >= map.size || y2 >= map.size) {
            println("Coordinate(s) out of array's bounds ($x1, $y1) ($x2, $y2)")
            return
        }

        val allNodes = mutableMapOf<Pair<Int, Int>, Node>()
        val startNode : Node = getOrCreateNode(Pair(x1, y1), map, allNodes)
        val destinationNode : Node = getOrCreateNode(Pair(x2, y2), map, allNodes)
        lastPath = find(startNode, destinationNode, map, allNodes)
    }

    public fun find(start : Node, destination : Node, map : Array<Array<Int>>, allNodes :  MutableMap<Pair<Int, Int>, Node>) : List<Node> {
        var found = false
        val path : MutableList<Node> = mutableListOf()

        val closed : MutableSet<Node> = mutableSetOf()
        val minHeap = PriorityQueue<Node>()
        minHeap.add(start)
        var c : Int = 0
        while (minHeap.isNotEmpty()) {
            c += 1
            if (c % 100000 == 0) println(c)
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
                    neighbours[i].heuristicCost = getDistance(current, neighbours[i])
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
        println("Количество созданных Node: " + allNodes.size)
        println("Количество итераций $c")
        println("Длина найденного пути: " + path.size)
        return path.reversed()
    }

    private fun getDistance(start : Node, destination : Node) : Int {
        val dX = abs(start.x - destination.x)
        val dY = abs(start.y - destination.y)
        return if (dX >= dY) 14 * dY + 10 * (dX - dY) else 14 * dX + 10 * (dY - dX)
    }

    private fun walkable(node : Node) : Boolean {
        return node.weight < 10
    }

    private fun getOrCreateNode(coords : Pair<Int, Int>, map : Array<Array<Int>>, allNodes :  MutableMap<Pair<Int, Int>, Node>) : Node {
        return allNodes.getOrPut(coords) {
            Node(coords.first, coords.second, 10 - map[coords.first][coords.second] * 10)
        }
    }
}

