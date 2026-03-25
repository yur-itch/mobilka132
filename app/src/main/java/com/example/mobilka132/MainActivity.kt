package com.example.mobilka132

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilka132.data.pathfinding.AStar
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    lateinit var mapManager : MapManager
    val viewModel: MapViewModel = MapViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapManager = MapManager(this)
        mapManager.loadData()
        viewModel.init(mapManager.grid)

        setContent {
            val state = viewModel.state
            val overlay = viewModel.overlay

            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            val maskBitmap = remember {
                val options = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(context.resources, R.drawable.map, options)
            }

            val dummyBitmap = remember {
                val options = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(context.resources, R.drawable.user_map_contrast, options)
            }

            LaunchedEffect(maskBitmap) {
                state.imageSize = Size(maskBitmap.width.toFloat(), maskBitmap.height.toFloat())
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (state.isProcessing) "Снаппинг к дороге..."
                        else if (state.isSelectionMode) "Выберите точку на карте"
                        else "Карта",
                        fontSize = 20.sp
                    )
                    if (state.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterEnd))
                    }
                }

                MapContainer(
                    state = state,
                    bitmap = dummyBitmap,
                    modifier = Modifier.weight(1f),
                    onPointSelected = { pressOffset ->
                        val contentPoint = state.screenToContent(pressOffset)
                        viewModel.onPointSelected(contentPoint, maskBitmap)
                    },
                    overlay = overlay,
                    viewModel
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { state.isSelectionMode = !state.isSelectionMode },
                        enabled = !state.isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isSelectionMode) Color.Red else Color.Blue
                        )
                    ) {
                        Text(if (state.isSelectionMode) "Отмена" else "Выбрать точку")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Точек: ${state.selectedPoints.size}")
                        Button(onClick = {
                            viewModel.clear()
                        }) {
                            Text("Очистить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapContainer(
    state: MapState,
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    onPointSelected: (Offset) -> Unit,
    overlay: MapOverlayRenderer,
    viewModel : MapViewModel
) {
    val cachedPath = remember(viewModel.lastPath) {
        overlay.generatePath(viewModel.lastPath)
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .fillMaxWidth()
            .background(Color.White)
            .onSizeChanged { state.containerSize = it }
            .pointerInput(state.isSelectionMode, state.isProcessing) {
                if (state.isSelectionMode && !state.isProcessing) {
                    detectTapGestures { onPointSelected(it) }
                } else {
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
                    drawPathScaled(cachedPath)
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            with(overlay) {
                drawMarkersUnscaled(state.selectedPoints)
            }
        }
    }
}
