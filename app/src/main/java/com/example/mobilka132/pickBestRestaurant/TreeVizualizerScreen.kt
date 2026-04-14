package com.example.mobilka132.pickBestRestaurant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class TreePosition(
    val node: DecisionTree.Node,
    val x: Float,
    val y: Float,
    val label: String,
    val edgeLabel: String?,
    val children: List<TreePosition>
)

@Composable
fun TreeVisualizerScreen(rootNode: DecisionTree.Node?) {
    if (rootNode == null) {
        Text("Дерево не построено", modifier = Modifier.padding(16.dp))
        return
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Визуализатор")
    }
}