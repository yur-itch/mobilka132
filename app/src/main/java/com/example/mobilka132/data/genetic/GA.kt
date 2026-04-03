package com.example.mobilka132.data.genetic

import kotlin.math.exp
import kotlin.math.log2
import kotlin.math.pow
import kotlin.random.Random

fun invertRange(arr: MutableList<Int>, from: Int, to: Int) {
    var l = minOf(from, to)
    var r = maxOf(from, to)
    while (l < r) {
        val tmp = arr[l]
        arr[l] = arr[r]
        arr[r] = tmp
        l++
        r--
    }
}

interface Distance {
    operator fun get(i: Int, j: Int): Double
    val size: Int
}

fun improvesWith2opt(
    arr: MutableList<Int>,
    ctx: MutationContext,
    i: Int,
    j: Int
): Boolean {
    if (j - i < 2) return false
    if (j >= arr.size - 1) return false

    val a = arr[i]
    val b = arr[i + 1]
    val c = arr[j]
    val d = arr[j + 1]

    val oldLen = ctx.dist[a, b] + ctx.dist[c, d]
    val newLen = ctx.dist[a, c] + ctx.dist[b, d]

    return newLen < oldLen
}

fun findFirst2opt(arr: MutableList<Int>, ctx: MutationContext): Pair<Int, Int>? {
    val n = arr.size
    for (i in 0 until n - 3) {
        for (j in i + 2 until n - 1) {
            if (improvesWith2opt(arr, ctx, i, j)) {
                return Pair(i, j)
            }
        }
    }
    return null
}

fun do2opt(arr: MutableList<Int>, ctx: MutationContext) {
    while (true) {
        val pair = findFirst2opt(arr, ctx) ?: break
        invertRange(arr, pair.first + 1, pair.second)
    }
}

fun randomInvert(arr: MutableList<Int>, ctx: MutationContext) {
    val left = Random.nextInt(1, arr.size)
    val right = Random.nextInt(1, arr.size)
    invertRange(arr, left, right)
}

fun randomRemove(arr: MutableList<Int>, ctx: MutationContext) {
    if (arr.size <= 1) return
    val index = Random.nextInt(1, arr.size)
    arr.removeAt(index)
}

fun randomAdd(arr: MutableList<Int>, ctx: MutationContext) {
    val isUsed = BooleanArray(ctx.allPoints.size)
    for (i in arr) {
        isUsed[i] = true
    }

    ctx.allPoints.shuffle()

    for (i in ctx.allPoints) {
        if (isUsed[i]) continue
        val pos = Random.nextInt(1, arr.size + 1)
        arr.add(pos, i)
        return
    }
}

data class MutationContext(
    val allPoints: MutableList<Int>,
    val dist: Distance,
    val items: MutableList<MutableList<Int>>,
    val allItems: MutableList<Int>,
    val initial: Int
)

typealias Mutation = (MutableList<Int>, MutationContext) -> Unit
typealias Population = MutableList<MutableList<Int>>

fun mutate(
    arr: MutableList<Int>,
    ctx: MutationContext,
    func: Mutation,
    decay: (Int, MutationContext) -> Double = { i, ctx ->
        val n = ctx.allPoints.size.toDouble()
        n / (n + 1) * exp(-i.toDouble() / log2(n + 1))
    }
) {
    var i = 0
    while (true) {
        val rand = Random.nextDouble()
        val chance = decay(i, ctx)
        if (rand >= chance) break
        i++
        func(arr, ctx)
    }
    if (Random.nextDouble() < 0.01) println(i)
}

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
    return 1 / (dist * factor)
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

fun performGeneration(pop: Population, ctx: MutationContext): MutableList<MutableList<Int>> {
    val sorted = pop.map { it to fitness(it, ctx) }
        .sortedByDescending { it.second }
        .map { it.first }
        .toMutableList()
    pop.clear()
    pop.addAll(sorted)
    val newPop = MutableList(pop.size / 20) { pop[it].toMutableList() }

    while (newPop.size < pop.size) {
        val one = Random.nextInt(0, pop.size)
        val two = Random.nextInt(0, pop.size)
        val fitnessOne = fitness(pop[one], ctx)
        val fitnessTwo = fitness(pop[two], ctx)
        val bestIndex = if (fitnessOne > fitnessTwo) one else two
        val best = pop[bestIndex].toMutableList()

        mutate(best, ctx, ::randomInvert)
        mutate(best, ctx, ::randomRemove)
        mutate(best, ctx, ::randomAdd)
        do2opt(best, ctx)

        newPop.add(best)
        // TODO: add crossover
    }

    return newPop
}

fun newPopulation(size: Int, ctx: MutationContext): Population {
    val nn = nearestNeighborAll(ctx)
    do2opt(nn, ctx)
    val res: Population = mutableListOf(nn)
    for (i in 1 until size) {
        val nnCopy = res[0].toMutableList()
        mutate(nnCopy, ctx, ::randomInvert)
        mutate(nnCopy, ctx, ::randomRemove)
        mutate(nnCopy, ctx, ::randomAdd)
        do2opt(nnCopy, ctx)
        res.add(nnCopy)
    }
    return res
}
