package com.example.mobilka132

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

class MapState {
    var offset by mutableStateOf(Offset.Zero)
    var scale by mutableFloatStateOf(1f)
    var containerSize by mutableStateOf(IntSize.Zero)

    fun updateTransform(centroid: Offset, pan: Offset, zoom: Float, imageSize: Size) {
        val oldScale = scale
        scale = (scale * zoom).coerceIn(1f, 50.0f)

        val factorX = containerSize.width / imageSize.width
        val factorY = containerSize.height / imageSize.height
        val fitScale = min(factorX, factorY)

        val mapWidthOnScreen = imageSize.width * fitScale
        val mapHeightOnScreen = imageSize.height * fitScale

        val extraSpaceX = (containerSize.width - mapWidthOnScreen) / 2f
        val extraSpaceY = (containerSize.height - mapHeightOnScreen) / 2f

        val panInContentSpace = pan / scale
        val zoomAdjustment = (centroid / scale) - (centroid / oldScale)
        val newOffset = offset + panInContentSpace + zoomAdjustment

        val centerX = containerSize.width / 2f
        val centerY = containerSize.height / 2f

        val maxX = (centerX / scale) - extraSpaceX
        val minX = (centerX / scale) - extraSpaceX - mapWidthOnScreen

        val maxY = (centerY / scale) - extraSpaceY
        val minY = (centerY / scale) - extraSpaceY - mapHeightOnScreen

        offset = Offset(
            x = newOffset.x.coerceIn(minX, maxX),
            y = newOffset.y.coerceIn(minY, maxY)
        )
    }

    fun screenToContent(screenOffset: Offset): Offset {
        return (screenOffset / scale) - offset
    }

    fun contentToScreen(contentOffset: Offset): Offset {
        return (contentOffset + offset) * scale
    }
}
