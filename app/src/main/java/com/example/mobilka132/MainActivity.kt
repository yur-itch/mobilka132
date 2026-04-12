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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

class MainActivity : ComponentActivity() {

    lateinit var mapManager: MapManager
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

            var showPointsList by remember { mutableStateOf(false) }

            val roadMask = remember {
                val options = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(context.resources, R.drawable.map, options)
            }
            val buildingsMask = remember {
                val options = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(context.resources, R.drawable.perfect_colored_map, options)
            }
            val dummyBitmap = remember {
                val options = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(context.resources, R.drawable.user_map_contrast, options)
            }

            val visibleBitmaps = arrayOf(dummyBitmap, roadMask)
            var shownIndex by remember { mutableIntStateOf(0) }

            LaunchedEffect(roadMask) {
                state.imageSize = Size(roadMask.width.toFloat(), roadMask.height.toFloat())
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (state.isProcessing) "Привязка к дороге..."
                        else if (state.isSelectionMode) "Выберите точку на карте"
                        else "Карта кампуса ТГУ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterEnd))
                    }
                }

                MapContainer(
                    state = state,
                    bitmap = visibleBitmaps[shownIndex],
                    modifier = Modifier.weight(1f),
                    onPointSelected = { screenPoint ->
                        viewModel.onPointSelected(screenPoint, roadMask, buildingsMask)
                    },
                    overlay = overlay,
                    viewModel = viewModel
                )

                if (state.selectedBuildingInfo != null) {
                    val info = state.selectedBuildingInfo!!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (info.name.isNotEmpty()) {
                                        Text(info.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(info.address, fontSize = 14.sp, color = Color.Gray)
                                }
                                IconButton(
                                    onClick = { state.selectedBuildingInfo = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            if (info.venues.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Заведения:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 180.dp)
                                ) {
                                    items(info.venues) { venue ->
                                        Text(
                                            text = "• $venue", 
                                            fontSize = 13.sp, 
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { state.isSelectionMode = !state.isSelectionMode },
                            enabled = !state.isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = if (state.isSelectionMode) Color.Red else Color.Blue)
                        ) {
                            Text(if (state.isSelectionMode) "Отмена" else "Выбрать точку")
                        }
                        Button(onClick = { showPointsList = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                            Text("Список точек")
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Точек: ${state.selectedPoints.size}", fontWeight = FontWeight.Bold)
                        Button(onClick = { viewModel.clear() }) {
                            Text("Очистить")
                        }
                        Button(onClick = { shownIndex = (shownIndex + 1) % visibleBitmaps.size }) {
                            Text("Сменить вид")
                        }
                    }
                }
            }

            if (showPointsList) {
                PointsListDialog(
                    points = state.selectedPoints,
                    onDismiss = { showPointsList = false },
                    onDeletePoint = { index -> viewModel.deletePoint(index) }
                )
            }
        }
    }
}

@Composable
fun PointsListDialog(points: List<MapPoint>, onDismiss: () -> Unit, onDeletePoint: (Int) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Список локаций", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                if (points.isEmpty()) {
                    Text("Список пуст")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        itemsIndexed(points) { index, point ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Точка №${point.id}")
                                IconButton(onClick = { onDeletePoint(index) }) { Text("✕", color = Color.Red) }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Закрыть") }
            }
        }
    }
}

@Composable
private fun MapContainer(state: MapState, bitmap: Bitmap, modifier: Modifier = Modifier, onPointSelected: (Offset) -> Unit, overlay: MapOverlayRenderer, viewModel: MapViewModel) {
    val textMeasurer = rememberTextMeasurer()
    val cachedPath = remember(viewModel.lastPath) { overlay.generatePath(viewModel.lastPath) }

    Box(
        modifier = modifier
            .clipToBounds()
            .fillMaxWidth()
            .background(Color.DarkGray)
            .onSizeChanged { state.containerSize = it }
            .pointerInput(state.isSelectionMode, state.isProcessing) {
                if (state.isSelectionMode && !state.isProcessing) {
                    detectTapGestures { onPointSelected(it) }
                } else {
                    detectTransformGestures { centroid, pan, zoom, _ -> state.updateTransform(centroid, pan, zoom) }
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = state.scale, scaleY = state.scale, translationX = state.offset.x * state.scale, translationY = state.offset.y * state.scale, transformOrigin = TransformOrigin(0f, 0f))) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            Canvas(modifier = Modifier.fillMaxSize()) { with(overlay) { drawPathScaled(cachedPath) } }
        }

        state.selectedPoints.forEach { point ->
            val screenPos = state.contentToScreen(point.position)
            val label = point.id.toString()
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color.Red, radius = 20f, center = screenPos)
                drawCircle(color = Color.White, radius = 8f, center = screenPos)
                val textLayoutResult = textMeasurer.measure(text = label, style = TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                val tw = textLayoutResult.size.width.toFloat()
                val th = textLayoutResult.size.height.toFloat()
                val pw = maxOf(50f, tw + 20f)
                val ph = 70f
                val tht = 25f
                val pbo = 25f
                val path = Path().apply {
                    moveTo(screenPos.x, screenPos.y - pbo)
                    lineTo(screenPos.x - pw / 2, screenPos.y - pbo - tht)
                    lineTo(screenPos.x - pw / 2, screenPos.y - pbo - ph)
                    lineTo(screenPos.x + pw / 2, screenPos.y - pbo - ph)
                    lineTo(screenPos.x + pw / 2, screenPos.y - pbo - tht)
                    close()
                }
                drawPath(path = path, color = Color.Yellow)
                drawPath(path = path, color = Color.Black, style = Stroke(width = 2f))
                drawText(textLayoutResult = textLayoutResult, topLeft = Offset(screenPos.x - tw / 2, screenPos.y - pbo - ph + (ph - tht) / 2 - th / 2))
            }
        }
    }
}
