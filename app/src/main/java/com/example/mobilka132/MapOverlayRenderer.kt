package com.example.mobilka132

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import com.example.mobilka132.data.pathfinding.Node

class MapOverlayRenderer(private val state: MapState) {
    fun DrawScope.drawPathScaled(pathNodes: List<Node>) {
        val pixelSize = state.fitScale * state.scale
        val pathColor = Color.Red

        pathNodes.forEach { node ->
            val screenPos = state.contentToScreen(Offset(node.x.toFloat(), node.y.toFloat()))
            drawRect(
                color = pathColor,
                topLeft = screenPos - Offset(pixelSize, pixelSize),
                size = Size(pixelSize * 3f, pixelSize * 3f)
            )
        }
    }

    fun DrawScope.drawMarkerUnscaled(point: Offset) {
        val screenPos = state.contentToScreen(point)
        drawCircle(color = Color.Blue, radius = 20f, center = screenPos)
        drawCircle(color = Color.White, radius = 10f, center = screenPos)
    }

    fun DrawScope.drawMarkersUnscaled(points: List<Offset>) = points.forEach { point -> drawMarkerUnscaled(point) }
}