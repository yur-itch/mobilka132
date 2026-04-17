package com.example.mobilka132.data.ant

import kotlin.math.hypot
import kotlin.random.Random

data class IntPoint(val x: Int, val y: Int)

data class CoworkingSpace(
    val id: Int,
    val position: IntPoint,
    val capacity: Int,
    val comfort: Double,
    var currentStudents: Int = 0
)

class PheromoneMap(val width: Int, val height: Int) {
    val toSpace = FloatArray(width * height)
    val toHome = FloatArray(width * height)

    fun getIndex(x: Int, y: Int): Int = y * width + x

    fun deposit(x: Int, y: Int, type: PheromoneType, amount: Float) {
        if (x in 0 until width && y in 0 until height) {
            val idx = getIndex(x, y)
            if (type == PheromoneType.TO_SPACE) toSpace[idx] += amount
            else toHome[idx] += amount
        }
    }

    fun evaporate(rate: Float) {
        for (i in toSpace.indices) {
            toSpace[i] *= (1f - rate)
            toHome[i] *= (1f - rate)
        }
    }
}

enum class PheromoneType { TO_SPACE, TO_HOME }

class Ant(var x: Int, var y: Int, private val campusWidth: Int, private val campusHeight: Int, private val grid: IntArray) {
    private var prevX: Int = x
    private var prevY: Int = y

    var hasFoundSpace: Boolean = false
    private var targetSpace: CoworkingSpace? = null
    private var stepsFromTarget = 1

    private val pathStack = mutableListOf<IntPoint>()

    private val neighbors = listOf(
        -1 to -1, 0 to -1, 1 to -1,
        -1 to  0,          1 to  0,
        -1 to  1, 0 to  1, 1 to  1
    )

    fun step(pheromones: PheromoneMap, spaces: List<CoworkingSpace>, startPos: IntPoint) {
        if (hasFoundSpace) {
            if (pathStack.isNotEmpty()) {
                val nextMove = pathStack.removeAt(pathStack.size - 1)
                prevX = x
                prevY = y
                x = nextMove.x
                y = nextMove.y
                stepsFromTarget++

                handlePheromonesAndTargets(pheromones, spaces, startPos)
            } else {
                hasFoundSpace = false
                targetSpace = null
                stepsFromTarget = 1
                pathStack.add(IntPoint(x, y))
            }
        } else {
            stepsFromTarget++
            val nextMove = chooseNextCell(pheromones)

            if (nextMove != null) {
                prevX = x
                prevY = y
                x = nextMove.x
                y = nextMove.y
            } else {
                val tempX = x; val tempY = y
                x = prevX; y = prevY
                prevX = tempX; prevY = tempY
            }

            val currentPos = IntPoint(x, y)
            val loopIndex = pathStack.indexOf(currentPos)
            if (loopIndex != -1) {
                while (pathStack.size > loopIndex + 1) {
                    pathStack.removeAt(pathStack.size - 1)
                }
                stepsFromTarget = pathStack.size
            } else {
                pathStack.add(currentPos)
            }

            handlePheromonesAndTargets(pheromones, spaces, startPos)
        }
    }

    private fun chooseNextCell(pheromones: PheromoneMap): IntPoint? {
        val candidates = mutableListOf<Pair<IntPoint, Double>>()

        val dirX = x - prevX
        val dirY = y - prevY

        for ((dx, dy) in neighbors) {
            val nextX = x + dx
            val nextY = y + dy

            if (isValid(nextX, nextY)) {
                if (nextX == prevX && nextY == prevY) continue

                var dirWeight = 1.0

                if (dirX != 0 || dirY != 0) {
                    val dotProduct = dx * dirX + dy * dirY
                    if (dotProduct > 0) {
                        dirWeight = 5.0
                    } else if (dotProduct == 0) {
                        dirWeight = 1.0
                    } else {
                        dirWeight = 0.1
                    }
                }

                dirWeight += Random.nextDouble(0.0, 0.5)

                val pWeight = pheromones.toSpace[nextY * campusWidth + nextX].toDouble()

                val finalWeight = dirWeight * (1.0 + pWeight * 100.0)

                candidates.add(IntPoint(nextX, nextY) to finalWeight)
            }
        }

        if (candidates.isEmpty()) return null

        val totalWeight = candidates.sumOf { it.second }
        var randomVal = Random.nextDouble() * totalWeight

        for (candidate in candidates) {
            randomVal -= candidate.second
            if (randomVal <= 0) return candidate.first
        }

        return candidates.lastOrNull()?.first
    }

    private fun isValid(nx: Int, ny: Int): Boolean {
        return nx in 0 until campusWidth &&
                ny in 0 until campusHeight &&
                grid[ny * campusWidth + nx] == 1
    }

    private fun handlePheromonesAndTargets(
        pheromones: PheromoneMap,
        spaces: List<CoworkingSpace>,
        startPos: IntPoint
    ) {
        if (!hasFoundSpace) {
            pheromones.deposit(x, y, PheromoneType.TO_HOME, 1f / (stepsFromTarget * stepsFromTarget))

            for (space in spaces) {
                val dist = hypot((x - space.position.x).toDouble(), (y - space.position.y).toDouble())

                if (dist < 10.0 && space.currentStudents < space.capacity) {
                    hasFoundSpace = true
                    targetSpace = space
                    space.currentStudents++
                    stepsFromTarget = 1
                    break
                }
            }
        } else {
            val quality = targetSpace?.comfort?.toFloat() ?: 0.5f

            val pheromoneAmount = quality * 100f / stepsFromTarget
            pheromones.deposit(x, y, PheromoneType.TO_SPACE, pheromoneAmount)

            val distToHome = hypot((x - startPos.x).toDouble(), (y - startPos.y).toDouble())
            if (distToHome < 2.0) {
                hasFoundSpace = false
                targetSpace = null
                stepsFromTarget = 1
                pathStack.clear()
                pathStack.add(IntPoint(x, y))
            }
        }
    }
}
class CampusSimulation(
    val width: Int,
    val height: Int,
    val grid: IntArray,
    var studentCount: Int = 30,
    customStartPosition: IntPoint? = null,
    customSpaces: List<CoworkingSpace>? = null
) {
    val pheromones = PheromoneMap(width, height)
    val spaces = mutableListOf<CoworkingSpace>()
    val ants = mutableListOf<Ant>()
    var startPosition = customStartPosition ?: IntPoint(width / 2, height / 2)

    init {
        if (customStartPosition == null) {
            while (grid[startPosition.y * width + startPosition.x] != 1) {
                startPosition = IntPoint(
                    Random.nextInt(100, (width - 100)),
                    Random.nextInt(100, (height - 100))
                )
            }
        }
        repeat(studentCount) {
            ants.add(Ant(startPosition.x, startPosition.y, width, height, grid))
        }
        if (!customSpaces.isNullOrEmpty()) {
            spaces.addAll(customSpaces)
        } else {
            generateRandomSpaces()
        }
    }

    private fun generateRandomSpaces() {
        var count = 0
        while (count < 10) {
            val rx = Random.nextInt(50, (width - 50))
            val ry = Random.nextInt(100, (height - 100))

            if (grid[ry * width + rx] == 1) {
                spaces.add(CoworkingSpace(
                    id = count,
                    position = IntPoint(rx, ry),
                    capacity = Random.nextInt(5, 50),
                    comfort = Random.nextDouble(0.3, 1.0)
                ))
                count++
            }
        }
    }

    fun update() {
        ants.forEach { it.step(pheromones, spaces, startPosition) }
        pheromones.evaporate(0.01f)
    }
}

