package com.example.mobilka132.data.genetic

import kotlin.random.Random

suspend fun nearestNeighborAll(ctx: MutationContext): MutableList<Int> {
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

suspend fun fitness(arr: MutableList<Int>, ctx: MutationContext): Double {
    val collected = BooleanArray(ctx.allItems.size) { false }
    for (i in 0 until arr.size) {
        val pointIndex = arr[i]
        for (item in ctx.items[pointIndex]) {
            collected[item] = true
        }
    }

    var dist = 0.0
    for (idx in 1 until arr.size) {
        dist += ctx.dist[arr[idx - 1], arr[idx]]
    }

    val uncollected = ctx.allItems.size - collected.count { it }
    return 1 / (dist + 1) - uncollected
}

suspend fun performGeneration(pop: Population, index: Int, total: Int, ctx: MutationContext): MutableList<MutableList<Int>> {
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
        // TODO: add crossover
    }

    return newPop
}

suspend fun newPopulation(size: Int, ctx: MutationContext): Population {
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


suspend fun printPopulationStatistics(population: Population, ctx: MutationContext, label: String) {
    val fitnesses = mutableListOf<Double>()
    for (p in population) {
        fitnesses.add(fitness(p, ctx))
    }

    val best = fitnesses.maxOrNull() ?: 0.0
    val worst = fitnesses.minOrNull() ?: 0.0
    val avg = fitnesses.average()
    val median = fitnesses.sorted().let {
        if (it.isEmpty()) 0.0
        else it[it.size / 2]
    }

    var bestRoute: MutableList<Int>? = null
    var maxFit = Double.NEGATIVE_INFINITY
    for (p in population) {
        val f = fitness(p, ctx)
        if (f > maxFit) {
            maxFit = f
            bestRoute = p
        }
    }

    val bestRouteLength = bestRoute?.size ?: 0

    val collectedItems = if (bestRoute != null) {
        val collected = mutableSetOf<Int>()
        for (i in bestRoute) {
            collected.addAll(ctx.items[i])
        }
        collected.size
    } else 0

    println("$label:")
    println("  Best fitness: %.10f".format(best))
    println("  Worst fitness: %.10f".format(worst))
    println("  Avg fitness: %.10f".format(avg))
    println("  Median fitness: %.10f".format(median))
    println("  Best route length: $bestRouteLength points")
    println("  Items collected by best route: $collectedItems / ${ctx.allItems.size}")
    println("  Population diversity: ${population.distinctBy { it.joinToString() }.size} unique routes")
    println()
}

suspend fun printDetailedRoute(route: MutableList<Int>, ctx: MutationContext, label: String) {
    val fit = fitness(route, ctx)
    var distance = 0.0
    for (idx in 1 until route.size) {
        distance += ctx.dist[route[idx - 1], route[idx]]
    }
    val collected = mutableSetOf<Int>()
    for (i in route) {
        collected.addAll(ctx.items[i])
    }

    println("$label:")
    println("  Fitness: $fit")
    println("  Distance: $distance")
    println("  Length: ${route.size}")
    println("  Items collected: ${collected.size}/${ctx.allItems.size}")
    println("  First 10 points: ${route.take(10)}")
    println()
}
