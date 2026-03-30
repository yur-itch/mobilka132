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
    fun generatePath(nodes: List<Offset>): Path {
        val path = Path()
        if (nodes.isEmpty()) return path

        path.moveTo(nodes[0].x, nodes[0].y)
        for (i in 1 until nodes.size) {
            path.lineTo(nodes[i].x, nodes[i].y)
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

    fun DrawScope.drawPointUnscaled(point: Offset, radius : Float = 5f, color : Color = Color.Yellow) {
        val screenPos = state.contentToScreen(point)
        drawCircle(color = color, radius = 2 * radius, center = screenPos)
        drawCircle(color = Color.White, radius = radius, center = screenPos)
    }

    fun DrawScope.drawPointsUnscaled(points: List<Offset>, radius : Float = 5f, color : Color = Color.Yellow) {
        points.forEach { drawPointUnscaled(it, radius, color) }
    }

    fun DrawScope.drawMarkersUnscaled(points: List<Offset>) =
        points.forEach { drawMarkerUnscaled(it) }
}