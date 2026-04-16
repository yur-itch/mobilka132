package com.example.mobilka132.pickBestRestaurant

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    val textMeasurer = rememberTextMeasurer()

    var scale by remember { mutableFloatStateOf(0.4f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var initialized by remember { mutableStateOf(false) }

    val treeLayout = remember(rootNode) {
        val leafCount = countLeaves(rootNode)
        val totalWidth = (leafCount * 1200f).coerceAtLeast(3000f)
        calculateNodePositions(
            node = rootNode,
            depth = 0,
            leftBounds = -totalWidth / 2f,
            rightBounds = totalWidth / 2f,
            edgeValue = null,
            parentKey = null
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color(0xFFF8F9FA))
            .onSizeChanged { size ->
                if (!initialized && size.width > 0) {
                    offset = Offset(size.width / 2f, 150f)
                    initialized = true
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    val newScale = (scale * zoom).coerceIn(0.05f, 4f)
                    offset = centroid - (centroid - offset) * (newScale / oldScale) + pan
                    scale = newScale
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        ) {
            drawTree(treeLayout, textMeasurer)
        }
    }
}

private fun countLeaves(node: DecisionTree.Node): Int {
    return when (node) {
        is DecisionTree.Leaf -> 1
        is DecisionTree.InternalNode -> {
            var count = 0
            node.branches.values.forEach { count += countLeaves(it) }
            count
        }
    }
}

private fun calculateNodePositions(
    node: DecisionTree.Node,
    depth: Int,
    leftBounds: Float,
    rightBounds: Float,
    edgeValue: String?,
    parentKey: String?
): TreePosition {
    val x = (leftBounds + rightBounds) / 2
    val y = depth * 800f

    val label = when (node) {
        is DecisionTree.Leaf -> node.result
        is DecisionTree.InternalNode -> translateKey(node.problemName) + "?"
    }

    val childrenPositions = mutableListOf<TreePosition>()

    if (node is DecisionTree.InternalNode) {
        val totalLeaves = countLeaves(node)
        var currentLeft = leftBounds
        val totalWidth = rightBounds - leftBounds

        node.branches.forEach { (answer, childNode) ->
            val childWeight = countLeaves(childNode)
            val childWidth = (childWeight.toFloat() / totalLeaves) * totalWidth

            val childPosition = calculateNodePositions(
                node = childNode,
                depth = depth + 1,
                leftBounds = currentLeft,
                rightBounds = currentLeft + childWidth,
                edgeValue = answer,
                parentKey = node.problemName
            )
            childrenPositions.add(childPosition)
            currentLeft += childWidth
        }
    }

    val translatedEdge = if (edgeValue != null && parentKey != null) {
        translateValue(parentKey, edgeValue)
    } else null

    return TreePosition(node, x, y, label, translatedEdge, childrenPositions)
}

private fun DrawScope.drawTree(position: TreePosition, textMeasurer: TextMeasurer) {
    val textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)

    val textLayout = textMeasurer.measure(
        text = position.label,
        style = textStyle
    )

    val horizontalPadding = 60f
    val verticalPadding = 40f
    val nodeWidth = textLayout.size.width + horizontalPadding
    val nodeHeight = textLayout.size.height + verticalPadding

    position.children.forEachIndexed { index, childPos ->
        drawLine(
            color = Color(0xFFADB5BD),
            start = Offset(position.x, position.y),
            end = Offset(childPos.x, childPos.y),
            strokeWidth = 6f
        )

        if (childPos.edgeLabel != null) {
            val lerpFactor = 0.5f
            val stagger = if (index % 2 == 0) -45f else 45f
            val textX = position.x + (childPos.x - position.x) * lerpFactor
            val textY = position.y + (childPos.y - position.y) * lerpFactor + stagger

            val edgeText = textMeasurer.measure(
                text = childPos.edgeLabel,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212529))
            )

            drawRoundRect(
                color = Color.White.copy(alpha = 0.95f),
                topLeft = Offset(textX - edgeText.size.width/2 - 10f, textY - edgeText.size.height/2 - 4f),
                size = Size(edgeText.size.width.toFloat() + 20f, edgeText.size.height.toFloat() + 8f),
                cornerRadius = CornerRadius(10f, 10f)
            )

            drawText(
                textLayoutResult = edgeText,
                topLeft = Offset(textX - edgeText.size.width / 2, textY - edgeText.size.height / 2)
            )
        }

        drawTree(childPos, textMeasurer)
    }

    val isLeaf = position.node is DecisionTree.Leaf

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.15f),
        topLeft = Offset(position.x - nodeWidth / 2 + 6f, position.y - nodeHeight / 2 + 6f),
        size = Size(nodeWidth, nodeHeight),
        cornerRadius = CornerRadius(24f, 24f)
    )

    drawRoundRect(
        color = if (isLeaf) Color(0xFF2D6A4F) else Color(0xFF0056B3),
        topLeft = Offset(position.x - nodeWidth / 2, position.y - nodeHeight / 2),
        size = Size(nodeWidth, nodeHeight),
        cornerRadius = CornerRadius(24f, 24f)
    )

    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(
            x = position.x - (textLayout.size.width / 2),
            y = position.y - (textLayout.size.height / 2)
        )
    )
}

private fun translateKey(key: String): String = when(key.trim().lowercase()) {
    "budget" -> "Бюджет"
    "location" -> "Локация"
    "time_available" -> "Время"
    "food_type" -> "Еда"
    "queue_tolerance" -> "Очередь"
    "weather" -> "Погода"
    else -> key
}

private fun translateValue(key: String, value: String): String {
    val k = key.trim().lowercase()
    return when(k) {
        "budget" -> when(value) {
            "low" -> "Низкий"
            "medium" -> "Средний"
            "high" -> "Высокий"
            else -> value
        }
        "time_available" -> when(value) {
            "very_short" -> "5 мин"
            "short" -> "15 мин"
            "medium" -> "30 мин"
            "long" -> "1 час+"
            else -> value
        }
        "location" -> when(value) {
            "main_building" -> "Гл. корпус"
            "second_building" -> "2 корпус"
            "campus_center" -> "Центр"
            "bus_stop" -> "Остановка"
            else -> value
        }
        "food_type" -> when(value) {
            "coffee" -> "Кофе"
            "pancakes" -> "Блины"
            "full_meal" -> "Обед"
            "snack" -> "Перекус"
            else -> value
        }
        "weather" -> if (value == "good") "Солнце" else "Дождь"
        "queue_tolerance" -> if (value == "high") "Готов" else "Нет"
        else -> value
    }
}
