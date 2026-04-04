package com.example.mobilka132.pickBestRestaurant

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.io.BufferedReader
import java.io.InputStreamReader


class DecisionTreeManager(application: Application) : AndroidViewModel(application) {
    private val treeManager = DecisionTree()
    private var rootNode: DecisionTree.Node? = null

    var currentNode by mutableStateOf<DecisionTree.Node?>(null)
    var recommendation by mutableStateOf("")
    var budgetInput by mutableStateOf("")

    init {
        reset()
    }

    fun reset() {
        val data = loadDataFromCsv()
        val attributes = listOf("location", "budget", "time_available", "food_type", "queue_tolerance", "weather")
        rootNode = treeManager.buildTree(data, attributes)
        currentNode = rootNode
        recommendation = ""
        budgetInput = ""
    }

    private fun loadDataFromCsv(): List<DecisionTree.Row> {
        val rows = mutableListOf<DecisionTree.Row>()
        try {
            val inputStream = getApplication<Application>().assets.open("restaurants.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val header = reader.readLine()
            
            reader.forEachLine { line ->
                val tokens = line.split(",")
                if (tokens.size == 7) {
                    val problems = mapOf(
                        "location" to tokens[0],
                        "budget" to tokens[1],
                        "time_available" to tokens[2],
                        "food_type" to tokens[3],
                        "queue_tolerance" to tokens[4],
                        "weather" to tokens[5]
                    )
                    rows.add(DecisionTree.Row(problems, tokens[6]))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rows
    }

    fun onAnswerSelected(answer: String) {
        val node = currentNode as? DecisionTree.InternalNode ?: return
        val nextNode = node.branches[answer]

        if (nextNode != null) {
            if (nextNode is DecisionTree.Leaf) {
                recommendation = nextNode.result
            }
            currentNode = nextNode
        }
    }

    fun onBudgetSubmitted() {
        val price = budgetInput.toDoubleOrNull() ?: 0.0
        val category = treeManager.categorizeBudget(price)
        onAnswerSelected(category)
    }

    fun stopAndGetCurrentPrediction() {
        val node = currentNode ?: return
        recommendation = treeManager.predict(node, emptyMap())
        currentNode = null
    }
}
