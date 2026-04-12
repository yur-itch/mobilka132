package com.example.mobilka132

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.example.mobilka132.model.MapPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class MapState {
    var offset by mutableStateOf(Offset.Zero)
    var scale by mutableFloatStateOf(1f)
    var containerSize by mutableStateOf(IntSize.Zero)
    var imageSize by mutableStateOf(Size.Zero)

    var maskWidth: Int = 0
    var maskHeight: Int = 0
    var maskPixels: IntArray = IntArray(0)

    val fitScale: Float
        get() = if (containerSize == IntSize.Zero || imageSize == Size.Zero) 1f
        else min(containerSize.width.toFloat() / imageSize.width, containerSize.height.toFloat() / imageSize.height)

    val extraSpaceX: Float
        get() = (containerSize.width - imageSize.width * fitScale) / 2f
    val extraSpaceY: Float
        get() = (containerSize.height - imageSize.height * fitScale) / 2f

    val selectedPoints = mutableStateListOf<MapPoint>()
    private var nextPointId = 1

    var isSelectionMode by mutableStateOf(false)
    var isProcessing by mutableStateOf(false)

    fun init(maskWidth: Int, maskHeight: Int, maskPixels: IntArray)
    {
        this.maskWidth = maskWidth
        this.maskHeight = maskHeight
        this.maskPixels = maskPixels
    }

    fun updateTransform(centroid: Offset, pan: Offset, zoom: Float) {
        if (imageSize == Size.Zero) return
        val oldScale = scale
        scale = (scale * zoom).coerceIn(1f, 50.0f)

        val mapWidthOnScreen = imageSize.width * fitScale
        val mapHeightOnScreen = imageSize.height * fitScale

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
        val inFittedSpace = (screenOffset / scale) - offset
        return Offset(
            x = (inFittedSpace.x - extraSpaceX) / fitScale,
            y = (inFittedSpace.y - extraSpaceY) / fitScale
        )
    }
    fun contentToScreen(contentOffset: Offset): Offset {
        val inFittedSpace = Offset(
            x = contentOffset.x * fitScale + extraSpaceX,
            y = contentOffset.y * fitScale + extraSpaceY
        )
        return (inFittedSpace + offset) * scale
    }

//    fun prepareMask(mask: Bitmap) {
//        if (maskPixels == null) {
//            maskWidth = mask.width
//            maskHeight = mask.height
//            println("$maskWidth $maskHeight")
//            val p = IntArray(maskWidth * maskHeight)
//            mask.getPixels(p, 0, maskWidth, 0, 0, maskWidth, maskHeight)
//        }
//    }

    fun addPointsDirectly(points: List<Offset>) {
        points.forEach { pt ->
            selectedPoints.add(MapPoint(id = nextPointId++, position = pt))
        }
    }

    suspend fun addPoint(contentPoint: Offset) = withContext(Dispatchers.Default) {
        isProcessing = true
        try {

            val finalPosition = findNearestAvailablePoint(contentPoint)
            withContext(Dispatchers.Main) {
                selectedPoints.add(MapPoint(id = nextPointId++, position = finalPosition))
                isSelectionMode = false
            }

        } finally {
            isProcessing = false
        }
    }

    fun findNearestAvailablePoint(startPoint: Offset): Offset {
        val pixels = maskPixels
        val w = maskWidth
        val h = maskHeight

        val centerX = startPoint.x.toInt().coerceIn(0, w - 1)
        val centerY = startPoint.y.toInt().coerceIn(0, h - 1)

        if (isColorWhite(pixels[centerY * w + centerX])) return startPoint

        val maxRadius = 1500
        for (radius in 1..maxRadius) {
            for (i in -radius..radius) {
                checkPixel(centerX + i, centerY - radius, w, h, pixels)?.let { return it }
                checkPixel(centerX + i, centerY + radius, w, h, pixels)?.let { return it }
                checkPixel(centerX - radius, centerY + i, w, h, pixels)?.let { return it }
                checkPixel(centerX + radius, centerY + i, w, h, pixels)?.let { return it }
            }
        }
        return startPoint
    }

    private fun checkPixel(x: Int, y: Int, w: Int, h: Int, pixels: IntArray): Offset? {
        if (x in 0 until w && y in 0 until h) {
            val color = pixels[y * w + x]
            if (color == 1) return Offset(x.toFloat(), y.toFloat())
        }
        return null
    }

    private fun isColorWhite(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return r > 200 && g > 200 && b > 200
    }

    fun clearPoints() {
        selectedPoints.clear()
        nextPointId = 1
    }
}
