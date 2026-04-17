package com.example.mobilka132.data.genetic

import kotlin.random.Random

fun nearestNeighborAll(ctx: MutationContext): MutableList<Int> {
    val res = MutableList(1) { ctx.initial }
    val used = MutableList(ctx.dist.size) { false }
    used[ctx.initial] = true
    while (res.size < ctx.dist.size) {
        var minDist = Double.POSITIVE_INFINITY
        var argMin = ctx.initial
        for (i in 0 until ctx.dist.size) {
            if (used[i]) continue
            val newDist = ctx.dist[res[res.size - 1], i]
            if (newDist < minDist) {
                minDist = newDist
                argMin = i
            }
        }
        res.add(argMin)
        used[argMin] = true
    }
    return res
}

fun fitness(arr: MutableList<Int>, ctx: MutationContext): Double {
    if (arr.isEmpty()) return -ctx.allItems.size.toDouble()

    val collected = BooleanArray(ctx.allItems.size) { false }
    var currentTime = ctx.startTime.toDouble()
    
    // First point
    val firstPointIdx = arr[0]
    val firstPoint = ctx.dist.getPoint(firstPointIdx)
    val firstTimeOfDay = currentTime % 1440
    if (firstTimeOfDay >= firstPoint.workingStart && firstTimeOfDay <= firstPoint.workingEnd) {
        for (item in ctx.items[firstPointIdx]) {
            if (item in 0 until ctx.allItems.size) {
                collected[item] = true
            }
        }
    }
    currentTime += firstPoint.delay

    for (idx in 1 until arr.size) {
        val prevIdx = arr[idx - 1]
        val currIdx = arr[idx]
        val d = ctx.dist[prevIdx, currIdx]
        
        // travel time = (pixels * metersPerPixel / 1000) / speedKmh * 60 min
        val travelTimeMinutes = (d * ctx.metersPerPixel * 60.0) / (ctx.speedKmh * 1000.0)
        currentTime += travelTimeMinutes
        
        val currPoint = ctx.dist.getPoint(currIdx)
        val timeOfDay = currentTime % 1440
        if (timeOfDay >= currPoint.workingStart && timeOfDay <= currPoint.workingEnd) {
            for (item in ctx.items[currIdx]) {
                if (item in 0 until ctx.allItems.size) {
                    collected[item] = true
                }
            }
        }
        currentTime += currPoint.delay
    }

    val totalTimeSpent = currentTime - ctx.startTime
    val collectedCount = collected.count { it }
    val uncollected = ctx.allItems.size - collectedCount

    return 1.0 / (totalTimeSpent + 1.0) - uncollected
}

fun performGeneration(pop: Population, index: Int, total: Int, ctx: MutationContext): MutableList<MutableList<Int>> {
    val fitnessPairs = mutableListOf<Pair<MutableList<Int>, Double>>()
    for (p in pop) {
        fitnessPairs.add(p to fitness(p, ctx))
    }

    val sorted = fitnessPairs
        .sortedByDescending { it.second }
        .map { it.first }
        .toMutableList()

    pop.clear()
    pop.addAll(sorted)
    val newPop = MutableList(pop.size / 20) { pop[it].toMutableList() }
    val mutationRate = 1.0 - (index / total.toDouble())

    while (newPop.size < pop.size) {
        val one = Random.nextInt(0, pop.size)
        val two = Random.nextInt(0, pop.size)
        val fitnessOne = fitness(pop[one], ctx)
        val fitnessTwo = fitness(pop[two], ctx)
        val bestIndex = if (fitnessOne > fitnessTwo) one else two
        newPop.add(Mutator(pop[bestIndex], ctx)
            .copy()
            .mutate({ invert() }, mutationRate)
            .mutate({ remove() }, mutationRate)
            .mutate({ add() }, mutationRate * 0.75)
            .do2opt()
            .get()
        )
    }

    return newPop
}

fun newPopulation(size: Int, ctx: MutationContext): Population {
    val nn = Mutator(nearestNeighborAll(ctx), ctx).do2opt().get()
    val res: Population = mutableListOf(nn)
    for (i in 1 until size) {
        res.add(Mutator(res[0], ctx)
            .copy()
            .mutate({ invert() }, 1.0)
            .mutate({ remove() }, 1.0)
            .mutate({ add() }, 1.0)
            .do2opt()
            .get()
        )
    }
    return res
}


fun printPopulationStatistics(population: Population, ctx: MutationContext, label: String) {
    val fitnesses = mutableListOf<Double>()
    for (p in population) {
        fitnesses.add(fitness(p, ctx))
    }

    val bestValue = fitnesses.maxOrNull() ?: 0.0
    val worstValue = fitnesses.minOrNull() ?: 0.0
    val avgValue = fitnesses.average()
    val medianValue = fitnesses.sorted().let {
        if (it.isEmpty()) 0.0
        else it[it.size / 2]
    }

    var bestRoute: MutableList<Int>? = null
    var maxFit = Double.NEGATIVE_INFINITY
    for (i in population.indices) {
        if (fitnesses[i] > maxFit) {
            maxFit = fitnesses[i]
            bestRoute = population[i]
        }
    }

    val bestRouteLength = bestRoute?.size ?: 0
    val collectedItemsCount = if (bestRoute != null) {
        getCollectedItemsCount(bestRoute, ctx)
    } else 0

    println("$label:")
    println("  Best fitness: %.10f".format(bestValue))
    println("  Worst fitness: %.10f".format(worstValue))
    println("  Avg fitness: %.10f".format(avgValue))
    println("  Median fitness: %.10f".format(medianValue))
    println("  Best route length: $bestRouteLength points")
    println("  Items collected by best route: $collectedItemsCount / ${ctx.allItems.size}")
    println("  Population diversity: ${population.distinctBy { it.joinToString() }.size} unique routes")
    println()
}

fun getCollectedItemsCount(route: MutableList<Int>, ctx: MutationContext): Int {
    if (route.isEmpty()) return 0
    val collected = mutableSetOf<Int>()
    var currentTime = ctx.startTime.toDouble()
    
    val firstPointIdx = route[0]
    val firstPoint = ctx.dist.getPoint(firstPointIdx)
    val firstTimeOfDay = currentTime % 1440
    if (firstTimeOfDay >= firstPoint.workingStart && firstTimeOfDay <= firstPoint.workingEnd) {
        collected.addAll(ctx.items[firstPointIdx])
    }
    currentTime += firstPoint.delay

    for (idx in 1 until route.size) {
        val d = ctx.dist[route[idx - 1], route[idx]]
        val travelTimeMinutes = (d * ctx.metersPerPixel * 60.0) / (ctx.speedKmh * 1000.0)
        currentTime += travelTimeMinutes
        
        val currIdx = route[idx]
        val currPoint = ctx.dist.getPoint(currIdx)
        val timeOfDay = currentTime % 1440
        if (timeOfDay >= currPoint.workingStart && timeOfDay <= currPoint.workingEnd) {
            collected.addAll(ctx.items[currIdx])
        }
        currentTime += currPoint.delay
    }
    return collected.size
}

fun printDetailedRoute(route: MutableList<Int>, ctx: MutationContext, label: String) {
    val fitValue = fitness(route, ctx)
    var distance = 0.0
    var currentTime = ctx.startTime.toDouble()
    val collected = mutableSetOf<Int>()

    if (route.isNotEmpty()) {
        val firstPointIdx = route[0]
        val firstPoint = ctx.dist.getPoint(firstPointIdx)
        val firstTimeOfDay = currentTime % 1440
        if (firstTimeOfDay >= firstPoint.workingStart && firstTimeOfDay <= firstPoint.workingEnd) {
            collected.addAll(ctx.items[firstPointIdx])
        }
        currentTime += firstPoint.delay

        for (idx in 1 until route.size) {
            val d = ctx.dist[route[idx - 1], route[idx]]
            distance += d
            val travelTimeMinutes = (d * ctx.metersPerPixel * 60.0) / (ctx.speedKmh * 1000.0)
            currentTime += travelTimeMinutes
            
            val currIdx = route[idx]
            val currPoint = ctx.dist.getPoint(currIdx)
            val timeOfDay = currentTime % 1440
            if (timeOfDay >= currPoint.workingStart && timeOfDay <= currPoint.workingEnd) {
                collected.addAll(ctx.items[currIdx])
            }
            currentTime += currPoint.delay
        }
    }

    println("$label:")
    println("  Fitness: $fitValue")
    println("  Distance: $distance")
    println("  Length: ${route.size}")
    println("  Items collected: ${collected.size}/${ctx.allItems.size}")
    val totalHours = currentTime.toInt() / 60
    val hours = totalHours % 24
    val minutes = currentTime.toInt() % 60
    val days = totalHours / 24
    if (days > 0) {
        println("  End Time: ${"%02d:%02d".format(hours, minutes)} (+ $days days)")
    } else {
        println("  End Time: ${"%02d:%02d".format(hours, minutes)}")
    }
    println("  First 10 points: ${route.take(10)}")
    println()
}
