package com.example.mobilka132.data.genetic

import kotlin.math.exp
import kotlin.math.log2
import kotlin.random.Random

data class MutationContext(
    val allPoints: MutableList<Int>,
    val dist: Distance,
    val items: MutableList<MutableList<Int>>,
    val allItems: MutableList<Int>,
    val initial: Int,
    val startTime: Int = 480, // Default to 8:00 AM
    val speedKmh: Double = 5.0,
    val metersPerPixel: Double = 0.5
)

typealias Population = MutableList<MutableList<Int>>

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
