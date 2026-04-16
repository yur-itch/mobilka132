package com.example.mobilka132.data.genetic

import kotlinx.coroutines.runBlocking
import kotlin.random.Random

/*
fun crossover(a1: MutableList<Int>, a2: MutableList<Int>) {
    return a1
}
*/

fun main() = runBlocking {
    println("=== Genetic Algorithm for TSP with Collection Problem ===\n")

    val numPoints = 50
    val numItems = 30
    val itemsPerPoint = 3
    val populationSize = 200
    val generations = 100

    println("Generating $numPoints random points with random working times...")
    val points = List(numPoints) { _ ->
        val start = Random.nextInt(480, 720) // Between 8:00 and 12:00
        val end = Random.nextInt(960, 1380)  // Between 16:00 and 23:00
        Point(
            x = Random.nextInt(0, 1000),
            y = Random.nextInt(0, 1000),
            workingStart = start,
            workingEnd = end
        )
    }

    val distancer = EucledianDistance(points)

    println("Generating $numItems items distributed across points...")
    val allItems = (0 until numItems).toMutableList()
    val items = MutableList(numPoints) { mutableListOf<Int>() }

    repeat(numPoints * itemsPerPoint) {
        val pointIdx = Random.nextInt(0, numPoints)
        val itemIdx = Random.nextInt(0, numItems)
        if (itemIdx !in items[pointIdx]) {
            items[pointIdx].add(itemIdx)
        }
    }

    val presentItems = items.flatten().toSet()
    val missingItems = allItems.filter { it !in presentItems }
    missingItems.forEach { item ->
        val randomPoint = Random.nextInt(0, numPoints)
        items[randomPoint].add(item)
    }

    println("\n=== Problem Statistics ===")
    println("Points: $numPoints")
    println("Items: $numItems")
    println("Items collected per point (avg): ${items.filter { it.isNotEmpty() }.map { it.size }.average()}")
    println("Points with no items: ${items.count { it.isEmpty() }}")

    val allPoints = (0 until numPoints).toMutableList()
    val initialPoint = Random.nextInt(0, numPoints)

    val ctx = MutationContext(
        allPoints = allPoints,
        dist = distancer,
        items = items,
        allItems = allItems,
        initial = initialPoint
    )

    println("\nInitial point: $initialPoint")
    println("\nInitializing population of size $populationSize...")
    var population = newPopulation(populationSize, ctx)
    printPopulationStatistics(population, ctx, "Initial Population")
    println("\nRunning $generations generations...\n")

    for (gen in 1..generations) {
        population = performGeneration(population, gen - 1, generations, ctx)
        if (gen % 10 == 0) {
            printPopulationStatistics(population, ctx, "Generation $gen")
        }
    }

    println("\n=== Final Results ===")
    
    var bestRoute: MutableList<Int>? = null
    var maxFitnessValue = Double.NEGATIVE_INFINITY
    for (p in population) {
        val f = fitness(p, ctx)
        if (f > maxFitnessValue) {
            maxFitnessValue = f
            bestRoute = p
        }
    }
    
    if (bestRoute != null) {
        printDetailedRoute(bestRoute, ctx, "Final Best Route")
    }
}
