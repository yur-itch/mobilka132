package com.example.mobilka132.data.genetic

import kotlin.math.exp
import kotlin.math.log2
import kotlin.math.pow
import kotlin.random.Random

class Mutator(private val path: MutableList<Int>, private val ctx: MutationContext) {
    fun get(): MutableList<Int> {
        return this.path
    }

    fun copy(): Mutator {
        return Mutator(this.path.toMutableList(), this.ctx)
    }

    fun invert(): Mutator {
        if (this.path.size <= 1) return this
        val left = Random.nextInt(1, this.path.size)
        val right = Random.nextInt(1, this.path.size)
        this.path.invertRange(left, right)
        return this
    }

    fun remove(): Mutator {
        if (this.path.size <= 1) return this
        val index = Random.nextInt(1, this.path.size)
        this.path.removeAt(index)
        return this
    }

    fun add(): Mutator {
        val isUsed = BooleanArray(this.ctx.allPoints.size)
        for (i in this.path) {
            isUsed[i] = true
        }

        this.ctx.allPoints.shuffle()

        for (i in ctx.allPoints) {
            if (isUsed[i]) continue
            val pos = Random.nextInt(1, this.path.size + 1)
            this.path.add(pos, i)
            return this
        }

        return this
    }

    fun do2opt(): Mutator {
        while (true) {
            val pair = this.path.findFirst2opt(ctx) ?: break
            this.path.invertRange(pair.first + 1, pair.second)
        }
        return this
    }

    fun expLogDecay(i: Int): Double {
        val n = this.ctx.allPoints.size.toDouble()
        val decay = (n / (n + 1) * exp(-i.toDouble() / log2(n + 1)))
        return decay
    }

    fun mutate(
        func: Mutator.() -> Mutator,
        strength: Double = 1.0,
        decay: Mutator.(Int) -> Double = { expLogDecay(it) },
    ): Mutator {
        var i = 0
        while (true) {
            val rand = Random.nextDouble()
            val chance = decay(i) * strength
            if (rand >= chance) break
            i++
            this.func()
        }
        return this
    }
}

fun <E> MutableList<E>.invertRange(from: Int, to: Int) {
    var l = minOf(from, to)
    var r = maxOf(from, to)
    while (l < r) {
        val tmp = this[l]
        this[l] = this[r]
        this[r] = tmp
        l++
        r--
    }
}

fun MutableList<Int>.improvesWith2opt(i: Int, j: Int, ctx: MutationContext): Boolean {
    if (j - i < 2) return false
    if (j >= this.size - 1) return false

    val a = this[i]
    val b = this[i + 1]
    val c = this[j]
    val d = this[j + 1]

    val oldLen = ctx.dist[a, b] + ctx.dist[c, d]
    val newLen = ctx.dist[a, c] + ctx.dist[b, d]

    return newLen < oldLen
}

interface Distance {
    operator fun get(i: Int, j: Int): Double
    val size: Int
}

fun MutableList<Int>.findFirst2opt(ctx: MutationContext): Pair<Int, Int>? {
    val n = this.size
    for (i in 0 until n - 3) {
        for (j in i + 2 until n - 1) {
            if (this.improvesWith2opt(i, j, ctx)) {
                return Pair(i, j)
            }
        }
    }
    return null
}

data class MutationContext(
    val allPoints: MutableList<Int>,
    val dist: Distance,
    val items: MutableList<MutableList<Int>>,
    val allItems: MutableList<Int>,
    val initial: Int
)

typealias Population = MutableList<MutableList<Int>>


/*
fun crossover(a1: MutableList<Int>, a2: MutableList<Int>) {
    return a1
}
*/

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
    val collected = BooleanArray(ctx.allItems.size) { false }
    for (i in 0 until arr.size) {
        val pointIndex = arr[i]
        for (item in ctx.items[pointIndex]) {
            collected[item] = true
        }
    }
    val dist = (1 until arr.size).sumOf { idx -> ctx.dist[arr[idx - 1], arr[idx]] }
    val uncollected = ctx.allItems.size - collected.count { it }
    val factor = uncollected + 1
    return 1 / (dist + 1) - uncollected
}

class Point(val x: Int, val y: Int) {
    fun distanceTo(other: Point): Double {
        return ((x - other.x).toDouble().pow(2) + (y - other.y).toDouble().pow(2)).pow(0.5)
    }
}

class Distancer(private val points: List<Point>) : Distance {
    override val size: Int = points.size

    operator override fun get(i: Int, j: Int): Double {
        return points[i].distanceTo(points[j])
    }
}

fun performGeneration(pop: Population, index: Int, total: Int, ctx: MutationContext): MutableList<MutableList<Int>> {
    val sorted = pop.map { it to fitness(it, ctx) }
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
            .mutate(Mutator::invert, mutationRate)
            .mutate(Mutator::remove, mutationRate)
            .mutate(Mutator::add, mutationRate)
            .do2opt()
            .get()
        )
        // TODO: add crossover
    }

    return newPop
}

fun newPopulation(size: Int, ctx: MutationContext): Population {
    val nn = Mutator(nearestNeighborAll(ctx), ctx).do2opt().get()
    val res: Population = mutableListOf(nn)
    for (i in 1 until size) {
        res.add(Mutator(res[0], ctx)
            .copy()
            .mutate(Mutator::invert, 1.0)
            .mutate(Mutator::remove, 1.0)
            .mutate(Mutator::add, 1.0)
            .do2opt()
            .get()
        )
    }
    return res
}


fun printPopulationStatistics(population: Population, ctx: MutationContext, label: String) {
    val fitnesses = population.map { fitness(it, ctx) }
    val best = fitnesses.maxOrNull() ?: 0.0
    val worst = fitnesses.minOrNull() ?: 0.0
    val avg = fitnesses.average()
    val median = fitnesses.sorted().let {
        if (it.isEmpty()) 0.0
        else it[it.size / 2]
    }

    val bestRoute = population.maxByOrNull { fitness(it, ctx) }
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

fun printDetailedRoute(route: MutableList<Int>, ctx: MutationContext, label: String) {
    val fit = fitness(route, ctx)
    val distance = (1 until route.size).sumOf { ctx.dist[route[it - 1], route[it]] }
    val collected = mutableSetOf<Int>()
    for (i in route) {
        collected.addAll(ctx.items[i])
    }

    println("$label:")
    println("  Fitness: $fit")
    println("  Distance: $distance")
    println("  Length: ${route.size}")
    println("  Items collected: ${collected.size}/${ctx.allItems.size}")
    println("  Items collected: ${collected.size}/${ctx.allItems.size}")
    println("  First 10 points: ${route.take(10)}")
    println()
}

fun main() {
    println("=== Genetic Algorithm for TSP with Collection Problem ===\n")

    val numPoints = 50
    val numItems = 30
    val itemsPerPoint = 3
    val populationSize = 200
    val generations = 100

    println("Generating $numPoints random points...")
    val points = List(numPoints) { index ->
        Point(
            x = Random.nextInt(0, 1000),
            y = Random.nextInt(0, 1000)
        )
    }

    val distancer = Distancer(points)

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

    val presentItems = items.flatMap { it }.toSet()
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
        population = performGeneration(population, gen - 1, generations,ctx)
        if (gen % 10 == 0) {
            printPopulationStatistics(population, ctx, "Generation $gen")
        }
    }

    println("\n=== Final Results ===")
    val bestRoute = population.maxByOrNull { fitness(it, ctx) }
    if (bestRoute != null) {
        val bestFitness = fitness(bestRoute, ctx)
        println("Best fitness: $bestFitness")
        println("\nBest route length: ${bestRoute.size} points")
        println("First 10 points of best route: ${bestRoute.take(10)}")
        val collectedItems = mutableSetOf<Int>()
        for (i in bestRoute) {
            collectedItems.addAll(items[i])
        }
        println("Items collected: ${collectedItems.size} / $numItems")
        println("Collection rate: ${(collectedItems.size.toDouble() / numItems * 100).toInt()}%")
        val totalDistance = (1 until bestRoute.size).sumOf {
            distancer[bestRoute[it - 1], bestRoute[it]]
        }
        println("Total travel distance: ${totalDistance.toInt()}")
        println("Starts at initial point: ${bestRoute[0] == initialPoint}")
        println("No duplicate points: ${bestRoute.distinct().size == bestRoute.size}")
    }
}
