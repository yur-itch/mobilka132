package com.example.mobilka132.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilka132.MapOverlayRenderer
import com.example.mobilka132.MapState
import com.example.mobilka132.MapViewModel
import com.example.mobilka132.data.clustering.BonusViewMode
import com.example.mobilka132.data.clustering.PointMultiAssignment
import com.example.mobilka132.data.location.LocationManager
import com.example.mobilka132.model.ObstacleLine

@Composable
fun MapContainer(
    state: MapState,
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    onPointSelected: (Offset) -> Unit,
    overlay: MapOverlayRenderer,
    viewModel: MapViewModel,
    location: LocationManager
) {
    val textMeasurer = rememberTextMeasurer()

    var draggingLine by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    val pathObjects = remember(viewModel.foundPaths.size) {
        viewModel.foundPaths.map { if (it.segments.isEmpty()) overlay.generatePath(it.steps) else null }
    }

    val stepOffset = remember(viewModel.currentStep) {
        viewModel.currentStep?.current?.let { (x, y) -> Offset(x, y) }
    }

    val nodeOffsets = remember(viewModel.currentStep) {
        viewModel.currentStep?.openSet?.map { (x, y) -> Offset(x, y) } ?: emptyList()
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { state.containerSize = it }
            .pointerInput(state.isSelectionMode, state.isProcessing) {
                detectTapGestures { offset ->
                    if (viewModel.bonusViewMode == BonusViewMode.DIFFERENCES &&
                        viewModel.bonusClusteringResult != null
                    ) {
                        val hit = viewModel.bonusClusteringResult!!.assignments
                            .minByOrNull { pt -> (state.contentToScreen(pt.position) - offset).getDistance() }
                            ?.let { pt ->
                                if ((state.contentToScreen(pt.position) - offset).getDistance() < 35f) pt else null
                            }
                        if (hit != null) {
                            viewModel.selectedBonusPoint = if (hit == viewModel.selectedBonusPoint) null else hit
                            return@detectTapGestures
                        }
                        viewModel.selectedBonusPoint = null
                    } else {
                        viewModel.selectedBonusPoint = null
                    }
                    if (!state.isProcessing && !viewModel.isObstacleMode && !viewModel.isProcessing) {
                        onPointSelected(offset)
                    }
                }
            }
            .pointerInput(viewModel.isObstacleMode) {
                if (viewModel.isObstacleMode) {
                    detectDragGestures(
                        onDragStart = { touchOffset ->
                            val contentPos = state.screenToContent(touchOffset)
                            val threshold = 50f / state.scale
                            val hit = viewModel.obstacles.firstNotNullOfOrNull { line ->
                                when {
                                    (contentPos - line.start).getDistance() < threshold -> line.id to true
                                    (contentPos - line.end).getDistance() < threshold -> line.id to false
                                    else -> null
                                }
                            }

                            if (hit != null) {
                                draggingLine = hit
                            } else {
                                val newId = (viewModel.obstacles.maxOfOrNull { it.id } ?: 0) + 1
                                val newLine = ObstacleLine(newId, contentPos, contentPos)
                                viewModel.addObstacle(newLine)
                                draggingLine = newId to false
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val contentPos = state.screenToContent(change.position)
                            draggingLine?.let { (id, isStart) ->
                                val index = viewModel.obstacles.indexOfFirst { it.id == id }
                                if (index != -1) {
                                    val line = viewModel.obstacles[index]
                                    viewModel.obstacles[index] = if (isStart) {
                                        line.copy(start = contentPos)
                                    } else {
                                        line.copy(end = contentPos)
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            draggingLine = null
                            viewModel.isObstacleMode = false
                            viewModel.syncObstacles()
                        }
                    )
                }
            }
            .pointerInput(viewModel.isObstacleMode) {
                if (!viewModel.isObstacleMode) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        state.updateTransform(centroid, pan, zoom)
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = state.scale,
                    scaleY = state.scale,
                    translationX = state.offset.x * state.scale,
                    translationY = state.offset.y * state.scale,
                    transformOrigin = TransformOrigin(0f, 0f)
                )
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                with(overlay) {
                    viewModel.foundPaths.forEachIndexed { index, path ->
                        if (path.segments.isNotEmpty()) {
                            drawPathSegmentsScaled(path.segments)
                        } else {
                            pathObjects.getOrNull(index)?.let { drawPathScaled(it) }
                        }
                    }

                    viewModel.tspPath?.steps?.let {
                        drawPathScaled(
                            generatePath(it),
                            color = Color(0xFF4CAF50)
                        )
                    }

                    viewModel.currentGAStep?.path?.let { gaPath ->
                        if (gaPath.segments.isNotEmpty()) {
                            drawPathSegmentsScaled(gaPath.segments, color = Color(0xFFFF9800))
                        } else {
                            drawPathScaled(generatePath(gaPath.steps), color = Color(0xFFFF9800))
                        }
                    }

                    withTransform({
                        translate(state.extraSpaceX, state.extraSpaceY)
                        scale(state.fitScale, state.fitScale, pivot = Offset.Zero)
                    }) {
                        viewModel.obstacles.forEach { line ->
                            drawLine(
                                color = Color.Red,
                                start = line.start,
                                end = line.end,
                                strokeWidth = 8f / state.scale,
                                cap = StrokeCap.Round
                            )
                            drawCircle(Color.Red, radius = 10f / state.scale, center = line.start)
                            drawCircle(Color.Red, radius = 10f / state.scale, center = line.end)
                        }

                        val frame = viewModel.currentSimulationFrame
                        frame.spaces.forEach { space ->
                            drawCircle(
                                color = if (space.currentStudents < space.capacity) Color.Green else Color.Red,
                                radius = 8f / state.scale,
                                center = Offset(space.position.x.toFloat(), space.position.y.toFloat())
                            )
                        }
                        frame.ants.forEach { ant ->
                            drawCircle(
                                color = if (ant.hasFoundSpace) Color.Yellow else Color.Cyan,
                                radius = 3f / state.scale,
                                center = Offset(ant.x.toFloat(), ant.y.toFloat())
                            )
                        }
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            with(overlay) {
                viewModel.userMapLocation?.let { drawPointUnscaled(it, 8f, primaryColor) }
                if (viewModel.currentStep != null) {
                    if (nodeOffsets.isNotEmpty()) drawPointsUnscaled(
                        nodeOffsets,
                        3f,
                        Color.Green.copy(alpha = 0.5f)
                    )
                    stepOffset?.let { drawPointUnscaled(it, 5f, Color.Yellow) }
                }
            }
        }

        val clusterColors = remember {
            listOf(
                Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFF8F00),
                Color(0xFF8E24AA), Color(0xFF00897B), Color(0xFFD81B60), Color(0xFF6D4C41)
            )
        }

        viewModel.bonusClusteringResult?.let { result ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (viewModel.bonusViewMode) {
                    BonusViewMode.EUCLIDEAN, BonusViewMode.MANHATTAN, BonusViewMode.ASTAR -> {
                        val (clusterOf, medoids) = when (viewModel.bonusViewMode) {
                            BonusViewMode.EUCLIDEAN -> Pair({ a: PointMultiAssignment -> a.euclideanCluster }, result.euclideanMedoids)
                            BonusViewMode.MANHATTAN -> Pair({ a: PointMultiAssignment -> a.manhattanCluster }, result.manhattanMedoids)
                            else                    -> Pair({ a: PointMultiAssignment -> a.astarCluster },     result.astarMedoids)
                        }
                        result.assignments.forEach { point ->
                            val sp = state.contentToScreen(point.position)
                            if (sp.x in 0f..size.width && sp.y in 0f..size.height) {
                                drawCircle(color = clusterColors.getOrElse(clusterOf(point)) { Color.Gray }.copy(alpha = 0.85f), radius = 9f, center = sp)
                                drawCircle(color = Color.White, radius = 3f, center = sp)
                            }
                        }
                        medoids.forEachIndexed { i, medoid ->
                            val sp = state.contentToScreen(medoid)
                            val color = clusterColors.getOrElse(i) { Color.Gray }
                            drawCircle(color = Color.White, radius = 20f, center = sp)
                            drawCircle(color = color,       radius = 15f, center = sp)
                            drawCircle(color = Color.White, radius =  5f, center = sp)
                        }
                    }
                    BonusViewMode.DIFFERENCES -> {
                        result.assignments.forEach { point ->
                            val sp = state.contentToScreen(point.position)
                            if (sp.x !in -20f..(size.width + 20f) || sp.y !in -20f..(size.height + 20f)) return@forEach
                            val r = 14f
                            val arcRect    = Size(r * 2, r * 2)
                            val arcTopLeft = androidx.compose.ui.geometry.Offset(sp.x - r, sp.y - r)
                            // Сектор Евклид (верх), Манхэттен (право), A* (лево)
                            drawArc(color = clusterColors.getOrElse(point.euclideanCluster) { Color.Gray }, startAngle = -90f, sweepAngle = 120f, useCenter = true, topLeft = arcTopLeft, size = arcRect)
                            drawArc(color = clusterColors.getOrElse(point.manhattanCluster) { Color.Gray }, startAngle =  30f, sweepAngle = 120f, useCenter = true, topLeft = arcTopLeft, size = arcRect)
                            drawArc(color = clusterColors.getOrElse(point.astarCluster)    { Color.Gray }, startAngle = 150f, sweepAngle = 120f, useCenter = true, topLeft = arcTopLeft, size = arcRect)
                            drawCircle(color = Color.White, radius = r, center = sp, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
                            drawCircle(color = Color.White, radius = 3f, center = sp)
                        }
                    }
                }
            }
        }

        val selPt = viewModel.selectedBonusPoint
        if (selPt != null && viewModel.bonusViewMode == BonusViewMode.DIFFERENCES) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 10.dp, tonalElevation = 4.dp) {
                    Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        @Composable fun MetricChip(label: String, idx: Int) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(28.dp).background(clusterColors.getOrElse(idx) { Color.Gray }, CircleShape))
                                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        MetricChip("Е",  selPt.euclideanCluster)
                        MetricChip("М",  selPt.manhattanCluster)
                        MetricChip("A*", selPt.astarCluster)
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            val visible = state.selectedPoints.mapNotNull { point ->
                val sp = state.contentToScreen(point.position)
                if (sp.x in 0f..size.width && sp.y in 0f..size.height) {
                    Triple(sp, point, textMeasurer.measure(point.id.toString(), textStyle))
                } else null
            }

            visible.forEach { (sp, _, _) ->
                drawCircle(color = primaryColor, radius = 15f, center = sp)
                drawCircle(color = Color.White, radius = 6f, center = sp)
            }

            visible.forEach { (sp, _, layout) ->
                val tw = layout.size.width.toFloat()
                val th = layout.size.height.toFloat()
                val labelBottom = sp.y - 19f
                val labelTop = labelBottom - th - 8f
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(sp.x - tw / 2 - 8f, labelTop),
                    size = Size(tw + 16f, th + 8f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawText(
                    textLayoutResult = layout,
                    color = Color.White,
                    topLeft = Offset(sp.x - tw / 2, labelTop + 4f)
                )
            }
        }
    }
}
