package com.example.mobilka132.pickBestRestaurant

import kotlin.math.log2

class DecisionTree {

    data class Row(
        val problems: Map<String, String>,
        val target: String
    )

    sealed class Node
    data class Leaf(val result: String) : Node()
    data class InternalNode(val problemName: String, val branches: Map<String, Node>) : Node()
    fun buildTree(rows: List<Row>, problems: List<String>, optimize: Boolean = false): Node {
        val root = buildRawTree(rows, problems)
        return if (optimize) optimization(root) else root
    }

    private fun buildRawTree(rows: List<Row>, problems: List<String>): Node {
        val distinctTargets = rows.map { it.target }.distinct()
        if (distinctTargets.size == 1) return Leaf(distinctTargets.first())

        if (problems.isEmpty()) {
            val mostCommon = rows.groupingBy { it.target }.eachCount()
                .maxByOrNull { it.value }?.key ?: "Unknown"
            return Leaf(mostCommon)
        }

        val bestProblem = problems.maxByOrNull { calculateGain(rows, it) } ?: problems.first()
        val remainingAttrs = problems - bestProblem

        val branches = rows.groupBy { it.problems[bestProblem] ?: "unknown" }
            .mapValues { (_, subset) -> buildRawTree(subset, remainingAttrs) }

        return InternalNode(bestProblem, branches)
    }
    private fun optimization(node: Node): Node {
        if (node is Leaf) return node
        if (node is InternalNode) {
            val optimizedBranches = node.branches.mapValues { optimization(it.value) }
            
            val firstChild = optimizedBranches.values.firstOrNull()
            if (firstChild is Leaf && optimizedBranches.values.all { it is Leaf && it.result == firstChild.result }) {
                return firstChild
            }
            return InternalNode(node.problemName, optimizedBranches)
        }
        return node
    }

    fun predict(node: Node, userFeatures: Map<String, String>): String {
        return when (node) {
            is Leaf -> node.result
            is InternalNode -> {
                val userValue = userFeatures[node.problemName]
                val nextNode = node.branches[userValue] ?: node.branches.values.firstOrNull()
                if (nextNode != null) {
                    predict(nextNode, userFeatures)
                } else {
                    "Ошибка: путь не найден"
                }
            }
        }
    }

    fun categorizeBudget(price: Double): String {
        return when {
            price < 500.0 -> "low"
            price < 1000.0 -> "medium"
            else -> "high"
        }
    }

    private fun calculateEntropy(rows: List<Row>): Double {
        val total = rows.size
        if (total == 0) return 0.0
        val counts = rows.groupingBy { it.target }.eachCount()
        var entropySum = 0.0
        for (count in counts.values) {
            val p = count.toDouble() / total
            if (p > 0) entropySum += p * log2(p)
        }
        return -entropySum
    }

    private fun calculateGain(rows: List<Row>, attrName: String): Double {
        val baseEntropy = calculateEntropy(rows)
        val totalSize = rows.size
        if (totalSize == 0) return 0.0

        val groups = rows.groupBy { row ->
            row.problems[attrName] ?: "unknown"
        }

        val subsetEntropy = groups.values.sumOf { subset ->
            (subset.size.toDouble() / totalSize) * calculateEntropy(subset)
        }

        return baseEntropy - subsetEntropy
    }
}
