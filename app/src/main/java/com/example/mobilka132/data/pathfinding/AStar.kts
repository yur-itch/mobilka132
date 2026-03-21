#!/usr/bin/env kotlin

import java.util.*
import kotlin.math.abs


class AStar {

    val grid : Array<Array<Node>>

    constructor(map : Array<Array<Int>>) {

        grid = arrayOfNulls<Array<Node>>(map.size) as Array<Array<Node>>

        for (i in 0 until map.size) {
            val row = arrayOfNulls<Node>(map[i].size)
            for (j in 0 until map[i].size) {
                row[j] = Node(i, j, 256 - map[i][j])
            }
            grid[i] = row as Array<Node>
        }
    }

    public fun find(start : Node, destination : Node) : List<Node> {
        var found = false
        val path : MutableList<Node> = mutableListOf()
        if (start.penalty == 256 && destination.penalty == 256) {
            return path;
        }
        val closed : MutableSet<Node> = mutableSetOf()
        val minHeap = PriorityQueue<Node>()
        minHeap.add(start)
        
        while (minHeap.isNotEmpty()) {
 			
            val current = minHeap.poll()
            closed.add(current)
            if (current == destination) {
                found = true
                break
            }
			
            val neighbours = mutableListOf<Node>()
            for (x in -1 until 2){
                for(y in -1 until 2){
                    val i : Int = x + current.x
                    val j : Int = y + current.y
                    if (i >= 0 && i < grid.size && j >= 0 && j < grid[current.x].size && !(x == 0 && y == 0)){
                        neighbours.add(grid[i][j])
                    }
                }
            }
            for (i in neighbours.indices){
                
                if (closed.contains(neighbours[i])){
                    continue
                }
                
                val newCost : Int = current.cost + getDistance(current, neighbours[i]) + neighbours[i].penalty
                if (newCost < neighbours[i].cost) {
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
        return path.reversed()
    }

    private fun getDistance(start : Node, destination : Node) : Int {
        val dX = abs(start.x - destination.x)
        val dY = abs(start.y - destination.y)
        return if (dX >= dY) 14 * dY + 10 * (dX - dY) else 14 * dX + 10 * (dY - dX)
    }
}

class Node : Comparable<Node> {

    val x : Int
    val y : Int


    var cost : Int = Int.MAX_VALUE
    var heuristicCost : Int = Int.MAX_VALUE
    val totalCost : Int
        get() = cost + heuristicCost
	var parent : Node? = null
    val penalty : Int

    constructor(x : Int, y: Int, penalty : Int) {
        this.x = x
        this.y = y
        this.penalty = penalty
    }

    override fun compareTo(other: Node): Int {
        val c = this.totalCost.compareTo(other.totalCost)
        if (c == 0) {
            return this.heuristicCost.compareTo(other.heuristicCost)
        }
        return -c
    }
}