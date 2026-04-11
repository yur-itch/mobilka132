package com.example.mobilka132.data.pathfinding
class Node : Comparable<Node> {

    val x : Int
    val y : Int

    var cost : Int = Int.MAX_VALUE
    var heuristicCost : Int = 0
    val totalCost : Int
        get() = cost + heuristicCost
	var parent : Node? = null
    val weight : Int

    constructor(x : Int, y: Int, weight : Int) {
        this.x = x
        this.y = y
        this.weight = weight
    }

    override fun compareTo(other: Node): Int {
        var c = this.totalCost.compareTo(other.totalCost)
        if (c == 0) {
            c = this.heuristicCost.compareTo(other.heuristicCost)
        }
        return c
    }
}