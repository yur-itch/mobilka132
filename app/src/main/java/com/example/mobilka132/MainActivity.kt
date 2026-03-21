package com.example.mobilka132

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Title", fontSize = 24.sp)

                MapContainer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {}) { Text("Button 1") }
                    Button(onClick = {}) { Text("Button 2") }
                }
            }
        }
    }
}

fun screenToContent(screenOffset: Offset, scale: Float, scrollOffset: Offset): Offset {
    return (screenOffset / scale) - scrollOffset
}

fun contentToScreen(contentOffset: Offset, scale: Float, scrollOffset: Offset): Offset {
    return (contentOffset + scrollOffset) * scale
}

@Composable
private fun MapContainer(modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val painter = painterResource(id = R.drawable.dummy_map)
    val imageIntrinsicSize = painter.intrinsicSize

    Box(
        modifier = modifier
            .clipToBounds()
            .fillMaxWidth()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    scale = (scale * zoom).coerceIn(0.5f, 5.0f)

                    val panInContentSpace = pan / scale
                    val zoomAdjustment = (centroid / scale) - (centroid / oldScale)
                    val newOffset = offset + panInContentSpace + zoomAdjustment

                    val factorX = containerSize.width / imageIntrinsicSize.width
                    val factorY = containerSize.height / imageIntrinsicSize.height
                    val fitScale = min(factorX, factorY)

                    val mapWidthOnScreen = imageIntrinsicSize.width * fitScale
                    val mapHeightOnScreen = imageIntrinsicSize.height * fitScale

                    val extraSpaceX = (containerSize.width - mapWidthOnScreen) / 2f
                    val extraSpaceY = (containerSize.height - mapHeightOnScreen) / 2f

                    val centerX = containerSize.width / 2f
                    val centerY = containerSize.height / 2f

                    val maxX = (centerX - extraSpaceX) / scale
                    val minX = maxX - (mapWidthOnScreen)

                    val maxY = (centerY - extraSpaceY) / scale
                    val minY = maxY - (mapHeightOnScreen)

                    offset = Offset(
                        x = newOffset.x.coerceIn(minX, maxX),
                        y = newOffset.y.coerceIn(minY, maxY)
                    )
                }
            }
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x * scale,
                    translationY = offset.y * scale,
                    transformOrigin = TransformOrigin(0f, 0f)
                )
        )
    }
}