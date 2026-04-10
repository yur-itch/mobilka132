package com.example.mobilka132.data.genetic

import com.example.mobilka132.data.pathfinding.AStar
import kotlinx.coroutines.runBlocking
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

    suspend fun do2opt(): Mutator {
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

    suspend fun mutate(
        func: suspend Mutator.() -> Mutator,
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

suspend fun MutableList<Int>.improvesWith2opt(i: Int, j: Int, ctx: MutationContext): Boolean {
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
    suspend operator fun get(i: Int, j: Int): Double
    val size: Int
}

suspend fun MutableList<Int>.findFirst2opt(ctx: MutationContext): Pair<Int, Int>? {
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

data class Point(val x: Int, val y: Int) {
    fun distanceTo(other: Point): Double {
        return ((x - other.x).toDouble().pow(2) + (y - other.y).toDouble().pow(2)).pow(0.5)
    }

    fun toPair(): Pair<Int, Int> {
        return Pair(this.x, this.y)
    }
}

class EucledianDistance(private val points: List<Point>) : Distance {
    override val size: Int = points.size

    override suspend operator fun get(i: Int, j: Int): Double {
        return points[i].distanceTo(points[j])
    }
}

data class CachedPath(
    val path: List<Point>,
    val length: Double
)

class WalkableDistance(
    private val algo: AStar
) : Distance {

    private var points: List<Point> = emptyList()
    override val size: Int get() = points.size

    private val coordinateCache = mutableMapOf<Pair<Point, Point>, CachedPath>()

    fun setPoints(newPoints: List<Point>) {
        this.points = newPoints
    }

    private fun key(p1: Point, p2: Point): Pair<Point, Point> {
        return if (p1.x < p2.x || (p1.x == p2.x && p1.y < p2.y)) p1 to p2 else p2 to p1
    }

    private suspend fun getCached(p1: Point, p2: Point): CachedPath {
        val k = key(p1, p2)
        coordinateCache[k]?.let { return it }

        val path = algo.find(k.first.toPair(), k.second.toPair())
        val cached = CachedPath(path = path.path.map { Point(it.x, it.y) }, length = path.distance.toDouble())

        coordinateCache[k] = cached
        return cached
    }

    override suspend operator fun get(i: Int, j: Int): Double =
        getCached(points[i], points[j]).length

    suspend fun path(i: Int, j: Int): List<Point> {
        val p1 = points[i]
        val p2 = points[j]
        val res = getCached(p1, p2)
        val k = key(p1, p2)
        return if (p1 == k.first) res.path else res.path.reversed()
    }
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
            .mutate({ add() }, mutationRate)
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

fun main() = runBlocking {
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
