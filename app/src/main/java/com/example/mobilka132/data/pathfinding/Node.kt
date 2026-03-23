package com.example.mobilka132.data.pathfinding
class Node : Comparable<Node> {

    val x : Short
    val y : Short

    var cost : Short = Short.MAX_VALUE
    var heuristicCost : Short = Short.MAX_VALUE
    val totalCost : Int
        get() = cost + heuristicCost
	var parent : Node? = null
    val penalty : Short

    constructor(x : Short, y: Short, penalty : Short) {
        this.x = x
        this.y = y
        this.penalty = penalty
    }

    override fun compareTo(other: Node): Int {
        var c = this.totalCost.compareTo(other.totalCost)
        if (c == 0) {
            c = this.heuristicCost.compareTo(other.heuristicCost)
        }
        return c
    }
}