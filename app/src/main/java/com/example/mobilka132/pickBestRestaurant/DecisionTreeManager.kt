package com.example.mobilka132.pickBestRestaurant

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class DecisionTreeManager(application: Application) : AndroidViewModel(application) {
    private val treeTool = DecisionTree()
    private var rootNode: DecisionTree.Node? = null

    var currentNode by mutableStateOf<DecisionTree.Node?>(null)
    var recommendation by mutableStateOf("")
    var budgetInput by mutableStateOf("")
    var decisionPath by mutableStateOf<List<Pair<String, String>>>(emptyList())

    var userCsvText by mutableStateOf("")
    var isSettingsMode by mutableStateOf(false)

    init {
        userCsvText = loadCsvFromAssets()
        reset()
    }

    fun reset() {
        val data = parseCsvFromText(userCsvText)
        val attributes = listOf("location", "budget", "time_available", "food_type", "queue_tolerance", "weather")
        rootNode = treeTool.buildTree(data, attributes, optimize = true)
        currentNode = rootNode
        recommendation = ""
        budgetInput = ""
        decisionPath = emptyList()
        isSettingsMode = false
    }

    private fun loadCsvFromAssets(): String {
        return try {
            getApplication<Application>().assets.open("restaurants.csv")
                .bufferedReader()
                .use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseCsvFromText(csvText: String): List<DecisionTree.Row> {
        val rows = mutableListOf<DecisionTree.Row>()
        val lines = csvText.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return rows

        lines.drop(1).forEach { line ->
            val tokens = line.split(",")
            if (tokens.size >= 7) {
                val problems = mapOf(
                    "location" to tokens[0].trim(),
                    "budget" to tokens[1].trim(),
                    "time_available" to tokens[2].trim(),
                    "food_type" to tokens[3].trim(),
                    "queue_tolerance" to tokens[4].trim(),
                    "weather" to tokens[5].trim()
                )
                rows.add(DecisionTree.Row(problems, tokens[6].trim()))
            }
        }
        return rows
    }

    fun onAnswerSelected(answer: String) {
        val node = currentNode as? DecisionTree.InternalNode ?: return
        val nextNode = node.branches[answer] ?: node.branches.values.firstOrNull()

        decisionPath = decisionPath + (node.problemName to answer)

        if (nextNode != null) {
            if (nextNode is DecisionTree.Leaf) {
                recommendation = nextNode.result
            }
            currentNode = nextNode
        }
    }

    fun onBudgetSubmitted() {
        val price = budgetInput.toDoubleOrNull() ?: 0.0
        val category = treeTool.categorizeBudget(price)
        onAnswerSelected(category)
    }

    fun stopAndGetCurrentPrediction() {
        val node = currentNode ?: return
        recommendation = treeTool.predict(node, emptyMap())
        currentNode = null
    }
}
