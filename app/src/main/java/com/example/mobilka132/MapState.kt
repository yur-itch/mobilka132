package com.example.mobilka132

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.example.mobilka132.model.MapPoint
import com.example.mobilka132.model.BuildingInfo
import com.example.mobilka132.model.VenueInfo
import com.example.mobilka132.model.MapPointData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

class MapState {
    companion object {
        const val REF_WIDTH = 3000f
        const val REF_HEIGHT = 3000f
        const val REF_MPP = 0.5
        const val REF_LON_SCALE = 122800.0
        const val REF_LAT_SCALE = 222000.0
        const val REF_SNAPPING_RADIUS = 1500
    }

    var offset by mutableStateOf(Offset.Zero)
    var scale by mutableFloatStateOf(1f)
    var containerSize by mutableStateOf(IntSize.Zero)
    var imageSize by mutableStateOf(Size.Zero)

    val scaleFactorX: Float get() = if (imageSize.width > 0) imageSize.width / REF_WIDTH else 1f
    val scaleFactorY: Float get() = if (imageSize.height > 0) imageSize.height / REF_HEIGHT else 1f

    val metersPerPixel: Double get() = REF_MPP / scaleFactorX.toDouble()
    val lonMultiplier: Double get() = REF_LON_SCALE * scaleFactorX.toDouble()
    val latMultiplier: Double get() = REF_LAT_SCALE * scaleFactorY.toDouble()
    val snappingRadius: Int get() = (REF_SNAPPING_RADIUS * scaleFactorX).toInt()

    var maskWidth: Int = 0
    var maskHeight: Int = 0
    var maskPixels: IntArray = IntArray(0)

    private var buildingsMaskPixels: IntArray? = null
    private var buildingsMaskWidth: Int = 0
    private var buildingsMaskHeight: Int = 0

    val fitScale: Float
        get() = if (containerSize == IntSize.Zero || imageSize == Size.Zero) 1f
        else min(
            containerSize.width.toFloat() / imageSize.width,
            containerSize.height.toFloat() / imageSize.height
        )

    val extraSpaceX: Float
        get() = (containerSize.width - imageSize.width * fitScale) / 2f
    val extraSpaceY: Float
        get() = (containerSize.height - imageSize.height * fitScale) / 2f

    val selectedPoints = mutableStateListOf<MapPoint>()
    private var nextPointId = 1

    var isSelectionMode by mutableStateOf(false)
    var isProcessing by mutableStateOf(false)

    var selectedBuildingInfo by mutableStateOf<BuildingInfo?>(null)
    var selectedVenueInfo by mutableStateOf<VenueInfo?>(null)
    var lastClickContentPoint by mutableStateOf<Offset?>(null)

    fun init(maskWidth: Int, maskHeight: Int, maskPixels: IntArray) {
        this.maskWidth = maskWidth
        this.maskHeight = maskHeight
        this.maskPixels = maskPixels
    }

    private fun prepareBuildingsMask(mask: Bitmap) {
        if (buildingsMaskPixels == null) {
            buildingsMaskWidth = mask.width
            buildingsMaskHeight = mask.height
            val p = IntArray(buildingsMaskWidth * buildingsMaskHeight)
            mask.getPixels(p, 0, buildingsMaskWidth, 0, 0, buildingsMaskWidth, buildingsMaskHeight)
            buildingsMaskPixels = p
        }
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

    fun addPointsDirectly(points: List<Offset>) {
        points.forEach { pt ->
            selectedPoints.add(MapPoint(id = nextPointId++, position = pt))
        }
    }

    fun addPointsWithTiming(points: List<MapPointData>) {
        points.forEach { p ->
            selectedPoints.add(
                MapPoint(
                    id = nextPointId++,
                    position = p.position,
                    workingStart = p.start,
                    workingEnd = p.end,
                    delay = p.delay,
                    items = p.items
                )
            )
        }
    }

    suspend fun addPoint(contentPoint: Offset): MapPoint? = withContext(Dispatchers.Default) {
        isProcessing = true
        try {
            val finalPosition = findNearestAvailablePoint(contentPoint)
            withContext(Dispatchers.Main) {
                val newPoint = MapPoint(id = nextPointId++, position = finalPosition)
                selectedPoints.add(newPoint)
                isSelectionMode = false
                newPoint
            }
        } catch (e: Exception) {
            null
        } finally {
            isProcessing = false
        }
    }

    suspend fun handleMapClick(
        contentPoint: Offset,
        roadMask: Bitmap?,
        buildingsMask: Bitmap?
    ) = withContext(Dispatchers.Default) {
        isProcessing = true
        lastClickContentPoint = contentPoint
        try {
            if (buildingsMask != null) {
                prepareBuildingsMask(buildingsMask)

                val bx = contentPoint.x.toInt().coerceIn(0, buildingsMaskWidth - 1)
                val by = contentPoint.y.toInt().coerceIn(0, buildingsMaskHeight - 1)

                val pixelColor = buildingsMaskPixels!![by * buildingsMaskWidth + bx]

                val buildingInfo =
                    if ((pixelColor and 0xFFFFFF) != 0xFFFFFF) {
                        CampusDatabase.getBuildingByColor(pixelColor)
                    } else {
                        null
                    }

                withContext(Dispatchers.Main) {
                    selectedBuildingInfo = buildingInfo
                    selectedVenueInfo = null
                }
            }

            if (isSelectionMode && (roadMask != null || maskPixels.isNotEmpty())) {
                val finalPosition = findNearestAvailablePoint(contentPoint)
                withContext(Dispatchers.Main) {
                    selectedPoints.add(MapPoint(id = nextPointId++, position = finalPosition))
                    isSelectionMode = false
                }
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

        if (pixels.isNotEmpty() && pixels[centerY * w + centerX] == 1) return startPoint

        val maxRadius = snappingRadius
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
        if (x in 0 until w && y in 0 until h && pixels.isNotEmpty()) {
            val color = pixels[y * w + x]
            if (color == 1) return Offset(x.toFloat(), y.toFloat())
        }
        return null
    }

    fun clearPoints() {
        selectedPoints.clear()
        nextPointId = 1
    }

    fun centerOnContent(contentPoint: Offset) {
        if (containerSize == IntSize.Zero || imageSize == Size.Zero) return
        val inFittedX = contentPoint.x * fitScale + extraSpaceX
        val inFittedY = contentPoint.y * fitScale + extraSpaceY
        val cx = containerSize.width / 2f
        val cy = containerSize.height / 2f
        val newOffsetX = cx / scale - inFittedX
        val newOffsetY = cy / scale - inFittedY
        val mapW = imageSize.width * fitScale
        val mapH = imageSize.height * fitScale
        offset = Offset(
            x = newOffsetX.coerceIn(cx / scale - extraSpaceX - mapW, cx / scale - extraSpaceX),
            y = newOffsetY.coerceIn(cy / scale - extraSpaceY - mapH, cy / scale - extraSpaceY)
        )
    }
}
