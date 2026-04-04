package com.example.mobilka132.pickBestRestaurant

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class DecisionTreeManager : ViewModel() {
    private val treeManager = DecisionTree()
    private var rootNode: DecisionTree.Node? = null

    var currentNode by mutableStateOf<DecisionTree.Node?>(null)
    var recommendation by mutableStateOf("")
    var budgetInput by mutableStateOf("")

    init {
        reset()
    }

    fun reset() {
        val data = treeManager.Data()
        val attributes = listOf("location", "budget", "time_available", "food_type", "queue_tolerance", "weather")
        rootNode = treeManager.buildTree(data, attributes)
        currentNode = rootNode
        recommendation = ""
        budgetInput = ""
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