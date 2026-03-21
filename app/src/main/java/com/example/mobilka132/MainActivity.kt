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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp

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

    Box(
        modifier = modifier
            .clipToBounds()
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    scale = (scale * zoom).coerceIn(0.5f, 5.0f)
                    val panInContentSpace = pan / scale
                    val zoomAdjustment = (centroid / scale) - (centroid / oldScale)
                    offset += panInContentSpace + zoomAdjustment
                }
            }
    ) {
        Image(
            painter = painterResource(R.drawable.dummy_map),
            contentDescription = null,
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