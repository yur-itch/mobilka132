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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.launch
import com.example.mobilka132.data.location.LocationManager

class MainActivity : ComponentActivity() {

    lateinit var mapManager: MapManager
    val viewModel: MapViewModel = MapViewModel()
    val location: LocationManager = LocationManager(this, activityResultRegistry)

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

            var showPointsList by remember { mutableStateOf(false) }

            val maskBitmap = remember {
                val options = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(context.resources, R.drawable.map, options)
            }

            val dummyBitmap = remember {
                val options = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(context.resources, R.drawable.user_map_contrast, options)
            }

            val bitmaps = arrayOf(maskBitmap, dummyBitmap)
            var shownIndex by remember { mutableIntStateOf(0) }

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
                        else "Интерактивная карта",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterEnd))
                    }
                }

                MapContainer(
                    state = state,
                    bitmap = bitmaps[shownIndex],
                    modifier = Modifier.weight(1f),
                    onPointSelected = { pressOffset ->
                        val contentPoint = state.screenToContent(pressOffset)
                        viewModel.onPointSelected(contentPoint, maskBitmap)
                    },
                    overlay = overlay,
                    viewModel,
                    location
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                            Text("Точек: ${state.selectedPoints.size}", fontWeight = FontWeight.Bold)

                            Button(onClick = { viewModel.clear() }) {
                                Text("Очистить")
                            }

                            Button(
                                onClick = { showPointsList = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                            ) {
                                Text("Список точек")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                if (viewModel.isPathProcessing)
                                    viewModel.cancelPathfinding()
                                else
                                    viewModel.requestPathfinding(true)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.isPathProcessing) Color.Red else Color.Blue
                            )
                        ) {
                            Text(if (viewModel.isPathProcessing) "Отмена" else "Найти путь")
                        }

                        Button(
                            onClick = {
                                if (viewModel.isPathProcessing)
                                    viewModel.cancelPathfinding()
                                else
                                    viewModel.requestPathfinding(false)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.isPathProcessing) Color.Red else Color.Blue
                            )
                        ) {
                            Text(if (viewModel.isPathProcessing) "Отмена" else "Найти путь быстро")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { location.requestNewLocationData() },
                            colors = ButtonDefaults.buttonColors(Color.Blue)
                        ) {
                            Text("Локация")
                        }

                        Button(
                            onClick = { shownIndex = (shownIndex + 1) % bitmaps.size },
                            colors = ButtonDefaults.buttonColors(Color.Blue)
                        ) {
                            Text("Сменить вид")
                        }
                    }
                }

                if (showPointsList) {
                    PointsListDialog(
                        points = state.selectedPoints,
                        onDismiss = { showPointsList = false },
                        onDeletePoint = { index -> viewModel.deletePoint(index) }
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { shownIndex = (shownIndex + 1) % bitmaps.size },
                            modifier = Modifier,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Blue
                            )
                        ) {
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
fun PointsListDialog(
    points: List<MapPoint>,
    onDismiss: () -> Unit,
    onDeletePoint: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Выбранные локации",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (points.isEmpty()) {
                    Text("Список пуст", modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        itemsIndexed(points) { index, point ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Точка №${point.id}", fontSize = 18.sp)
                                IconButton(onClick = { onDeletePoint(index) }) {
                                    Text("✕", color = Color.Red, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            HorizontalDivider(color = Color.LightGray)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).padding(top = 16.dp)
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}

