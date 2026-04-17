package com.example.mobilka132.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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

                    viewModel.tspPath?.steps?.let { drawPathScaled(generatePath(it), color = Color(0xFF4CAF50)) }
                    
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
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            with(overlay) {
                location.mapLocation?.let { drawPointUnscaled(it, 8f, primaryColor) }
                if (viewModel.currentStep != null) {
                    if (nodeOffsets.isNotEmpty()) drawPointsUnscaled(nodeOffsets, 3f, Color.Green.copy(alpha = 0.5f))
                    stepOffset?.let { drawPointUnscaled(it, 5f, Color.Yellow) }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            state.selectedPoints.forEach { point ->
                val screenPos = state.contentToScreen(point.position)

                if (screenPos.x in 0f..size.width && screenPos.y in 0f..size.height) {
                    drawCircle(color = primaryColor, radius = 15f, center = screenPos)
                    drawCircle(color = Color.White, radius = 6f, center = screenPos)

                    val textLayoutResult = textMeasurer.measure(point.id.toString(), textStyle)
                    val textWidth = textLayoutResult.size.width.toFloat()
                    val textHeight = textLayoutResult.size.height.toFloat()

                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(screenPos.x - textWidth / 2 - 8f, screenPos.y - 45f),
                        size = Size(textWidth + 16f, textHeight + 8f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        color = Color.White,
                        topLeft = Offset(screenPos.x - textWidth / 2, screenPos.y - 41f)
                    )
                }
            }
        }
    }
}
