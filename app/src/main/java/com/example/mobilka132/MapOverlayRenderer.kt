package com.example.mobilka132

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.mobilka132.data.pathfinding.Node

class MapOverlayRenderer(private val state: MapState) {
    fun generatePath(nodes: List<Node>): Path {
        val path = Path()
        if (nodes.isEmpty()) return path

        path.moveTo(nodes[0].x.toFloat(), nodes[0].y.toFloat())
        for (i in 1 until nodes.size) {
            path.lineTo(nodes[i].x.toFloat(), nodes[i].y.toFloat())
        }
        return path
    }

    fun DrawScope.drawPathScaled(
        path: Path,
        color: Color = Color.Red,
        thickness: Float = 3f
    ) {
        withTransform({
            translate(state.extraSpaceX, state.extraSpaceY)
            scale(state.fitScale, state.fitScale, pivot = Offset.Zero)
        }) {
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = thickness / state.fitScale)
            )
        }
    }

    fun DrawScope.drawMarkerUnscaled(point: Offset) {
        val screenPos = state.contentToScreen(point)
        drawCircle(color = Color.Blue, radius = 20f, center = screenPos)
        drawCircle(color = Color.White, radius = 10f, center = screenPos)
    }

    fun DrawScope.drawMarkersUnscaled(points: List<Offset>) =
        points.forEach { drawMarkerUnscaled(it) }
}