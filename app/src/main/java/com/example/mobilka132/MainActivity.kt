package com.example.mobilka132

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobilka132.pickBestRestaurant.DecisionTreeManager
import com.example.mobilka132.pickBestRestaurant.DecisionDialog
import com.example.mobilka132.data.location.LocationManager
import com.example.mobilka132.model.MapPoint
import kotlinx.coroutines.CoroutineScope

class MainActivity : ComponentActivity() {

    private lateinit var mapManager: MapManager
//    private val viewModel: MapViewModel = MapViewModel()
    private val viewModel: MapViewModel by viewModels<MapViewModel>()
    private val location: LocationManager by lazy { LocationManager(this, activityResultRegistry) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapManager = MapManager(this)
        mapManager.loadData()
        mapManager.loadPointsFromAssets()
        viewModel.init(mapManager)
        location.checkPermission()

        setContent {
            val state = viewModel.state
            val overlay = viewModel.overlay
            val context = LocalContext.current
            val treeViewModel: DecisionTreeManager = viewModel()

            var showDecisionDialog by remember { mutableStateOf(false) }
            var showPointsList by remember { mutableStateOf(false) }
            var showRouteMenu by remember { mutableStateOf(false) }

            var startPoint by remember { mutableStateOf<Offset?>(null) }
            var endPoint by remember { mutableStateOf<Offset?>(null) }
            var startLabel by remember { mutableStateOf("Выберите начало") }
            var endLabel by remember { mutableStateOf("Выберите конец") }
            var visualizeRoute by remember { mutableStateOf(false) }
            var stepDelay by remember {mutableStateOf(5L)}

            val maskBitmap = remember {
                val options = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(context.resources, R.drawable.map, options)
            }
            val dummyBitmap = remember {
                val options = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(context.resources, R.drawable.user_map_contrast, options)
            }
            val bitmaps = arrayOf(maskBitmap, dummyBitmap)
            var shownIndex by remember { mutableIntStateOf(1) }

            LaunchedEffect(maskBitmap) {
                state.imageSize = Size(maskBitmap.width.toFloat(), maskBitmap.height.toFloat())
            }

            Scaffold(
                modifier = Modifier
                    .fillMaxSize(),
//                contentWindowInsets = WindowInsets(),
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HeaderSection(state, viewModel)

                    if (showRouteMenu) {
                        RouteMenu(
                            points = state.selectedPoints,
                            myLocation = location.mapLocation,
                            startLabel = startLabel,
                            endLabel = endLabel,
                            isVisualized = visualizeRoute,
                            stepDelay = stepDelay,
                            onStepDelayChange = {stepDelay = it},
                            onVisualizationToggle = { visualizeRoute = it },
                            onStartSelected = { offset, label ->
                                startPoint = offset; startLabel = label
                            },
                            onEndSelected = { offset, label ->
                                endPoint = offset; endLabel = label
                            },
                            onClose = { showRouteMenu = false },
                            onBuildRoute = {
                                if (startPoint != null && endPoint != null) {
                                    viewModel.requestPathfinding(
                                        state.findNearestAvailablePoint(
                                            startPoint!!
                                        ), endPoint!!, visualizeRoute,
                                        stepDelay
                                    )
                                    showRouteMenu = false
                                }
                            }
                        )
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
                        viewModel = viewModel,
                        location = location
                    )

                    ControlPanel(
                        state = state,
                        viewModel = viewModel,
                        onShowPointsList = { showPointsList = true },
                        onShowDecisionDialog = { showDecisionDialog = true },
                        onToggleView = { shownIndex = (shownIndex + 1) % bitmaps.size },
                        onToggleRouteMenu = { showRouteMenu = !showRouteMenu },
                        maskBitmap = maskBitmap
                    )

                    if (showPointsList) {
                        PointsListDialog(
                            points = state.selectedPoints,
                            onDismiss = { showPointsList = false },
                            onDeletePoint = { index -> viewModel.deletePoint(index) }
                        )
                    }

                    if (showDecisionDialog) {
                        DecisionDialog(
                            viewModel = treeViewModel,
                            onDismiss = { showDecisionDialog = false }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun HeaderSection(state: MapState, viewModel: MapViewModel) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Text(
                text = when {
                    state.isProcessing -> "Снаппинг к дороге..."
                    state.isSelectionMode -> "Выберите точку на карте"
                    viewModel.isGARunning -> "Генетика: ген. ${viewModel.currentGeneration}"
                    viewModel.isPathProcessing -> "Поиск пути..."
                    else -> "Интерактивная карта"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (state.isProcessing || viewModel.activeJobs.isNotEmpty()) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp).align(Alignment.CenterEnd).padding(end = 16.dp))
            }
        }
    }

    @Composable
    private fun ControlPanel(
        state: MapState,
        viewModel: MapViewModel,
        onShowPointsList: () -> Unit,
        onShowDecisionDialog: () -> Unit,
        onToggleView: () -> Unit,
        onToggleRouteMenu: () -> Unit,
        maskBitmap: Bitmap
    ) {
        val isBusy = viewModel.activeJobs.isNotEmpty() || state.isProcessing
        println(isBusy)
        Column(modifier = Modifier.padding(8.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp)).padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = onToggleRouteMenu, enabled = !isBusy) { Text("Маршрут") }
                Button(onClick = onToggleView) { Text("Вид") }
                Button(onClick = { viewModel.startFoodShoppingGA(maskBitmap) }, enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(Color(0xFFFF9800))) {
                    Text("GA")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { state.isSelectionMode = !state.isSelectionMode },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.isSelectionMode) Color.Red else Color(0xFF2196F3))
                ) {
                    Text(if (state.isSelectionMode) "Отмена" else "Точка")
                }

                Button(onClick = onShowPointsList, colors = ButtonDefaults.buttonColors(Color.Gray)) {
                    Text("Список (${state.selectedPoints.size})")
                }

                Button(
                    onClick = { viewModel.clear() },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) {
                    Text("Оч.", fontSize = 20.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = onShowDecisionDialog, colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))) {
                    Text("💡 Совет")
                }
                if (viewModel.activeJobs.isNotEmpty()) {
                    Button(onClick = { viewModel.cancelAll() }, colors = ButtonDefaults.buttonColors(Color.Red)) {
                        Text("Остановить")
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
        viewModel: MapViewModel,
        location: LocationManager
    ) {
        val textMeasurer = rememberTextMeasurer()
        val cachedPath = remember(viewModel.lastPath?.steps) {
            overlay.generatePath(viewModel.lastPath?.steps)
        }
        val stepOffset = remember(viewModel.currentStep) {
            viewModel.currentStep?.current
        }
        val nodeOffsets = remember(viewModel.currentStep) {
            viewModel.currentStep?.openSet ?: emptyList()
        }

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
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            state.updateTransform(centroid, pan, zoom)
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier.fillMaxSize().graphicsLayer(
                    scaleX = state.scale, scaleY = state.scale,
                    translationX = state.offset.x * state.scale,
                    translationY = state.offset.y * state.scale,
                    transformOrigin = TransformOrigin(0f, 0f)
                )
            ) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                Canvas(modifier = Modifier.fillMaxSize()) {
                    with(overlay) { drawPathScaled(cachedPath) }
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                with(overlay) {
                    location.mapLocation?.let { drawPointUnscaled(it, 7f, Color.Yellow) }
                    if (viewModel.currentStep != null) {
                        drawPointsUnscaled(nodeOffsets, 3f, Color.Green)
                        stepOffset?.let { drawPointUnscaled(it, 5f, Color.Yellow) }
                    }
                }
            }

            FilledIconButton(
                onClick = { location.requestNewLocationData() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(if (location.mapLocation == null) Color.Blue else Color.Gray)
            ) {
                Text("GPS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val textStyle = TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                state.selectedPoints.forEach { point ->
                    val screenPos = state.contentToScreen(point.position)
                    drawCircle(color = Color.Red, radius = 20f, center = screenPos)
                    drawCircle(color = Color.White, radius = 8f, center = screenPos)

                    val textLayoutResult = textMeasurer.measure(text = point.id.toString(), style = textStyle)
                    val textWidth = textLayoutResult.size.width.toFloat()
                    val textHeight = textLayoutResult.size.height.toFloat()
                    val pinWidth = maxOf(50f, textWidth + 20f)
                    val pinHeight = 70f
                    val tailHeight = 25f
                    val pinBottomOffset = 25f

                    val path = Path().apply {
                        moveTo(screenPos.x, screenPos.y - pinBottomOffset)
                        lineTo(screenPos.x - pinWidth / 2, screenPos.y - pinBottomOffset - tailHeight)
                        lineTo(screenPos.x - pinWidth / 2, screenPos.y - pinBottomOffset - pinHeight)
                        lineTo(screenPos.x + pinWidth / 2, screenPos.y - pinBottomOffset - pinHeight)
                        lineTo(screenPos.x + pinWidth / 2, screenPos.y - pinBottomOffset - tailHeight)
                        close()
                    }
                    drawPath(path = path, color = Color.Yellow)
                    drawPath(path = path, color = Color.Black, style = Stroke(width = 2f))
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = screenPos.x - textWidth / 2,
                            y = screenPos.y - pinBottomOffset - pinHeight + (pinHeight - tailHeight) / 2 - textHeight / 2
                        )
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
                    Text("Выбранные локации", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (points.isEmpty()) {
                        Text("Список пуст", modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            itemsIndexed(points) { index, point ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Точка №${point.id}", fontSize = 18.sp)
                                    IconButton(onClick = { onDeletePoint(index) }) {
                                        Text("✕", color = Color.Red, fontSize = 20.sp)
                                    }
                                }
                                HorizontalDivider(color = Color.LightGray)
                            }
                        }
                    }
                    Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(top = 16.dp)) { Text("Закрыть") }
                }
            }
        }
    }
}

@Composable
fun RouteMenu(
    points: List<MapPoint>,
    myLocation: Offset?,
    startLabel: String,
    endLabel: String,
    isVisualized: Boolean,
    stepDelay: Long,
    onStepDelayChange: (Long) -> Unit,
    onVisualizationToggle: (Boolean) -> Unit,
    onStartSelected: (Offset, String) -> Unit,
    onEndSelected: (Offset, String) -> Unit,
    onBuildRoute: () -> Unit,
    onClose: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth().padding(8.dp), shape = RoundedCornerShape(16.dp), shadowElevation = 8.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Маршрут", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(12.dp))

            PointSelector("От: $startLabel", points, myLocation, onStartSelected)
            Spacer(modifier = Modifier.height(8.dp))
            PointSelector("До: $endLabel", points, null, onEndSelected)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = isVisualized,
                    onCheckedChange = onVisualizationToggle
                )
                Text("A*", fontSize = 14.sp)

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Задержка: ${stepDelay.toInt()} мс", fontSize = 12.sp, color = Color.Gray)
                    Slider(
                        value = stepDelay.toFloat(),
                        onValueChange = { onStepDelayChange(it.toLong()) },
                        valueRange = 0f..50f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF4CAF50), activeTrackColor = Color(0xFF4CAF50))
                    )
                }
            }


            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onClose) { Text("Закрыть", color = Color.Gray) }
                Button(
                    onClick = onBuildRoute,
                    enabled = startLabel != "Выберите начало" && endLabel != "Выберите конец",
                    colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))
                ) {
                    Text("Построить")
                }
            }
        }
    }
}

@Composable
fun PointSelector(label: String, points: List<MapPoint>, myLocation: Offset?, onSelected: (Offset, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            myLocation?.let {
                DropdownMenuItem(text = { Text("Моя локация (GPS)") }, onClick = { onSelected(it, "Моя локация"); expanded = false })
            }
            points.forEach { point ->
                DropdownMenuItem(text = { Text("Точка №${point.id}") }, onClick = { onSelected(point.position, "Точка №${point.id}"); expanded = false })
            }
        }
    }
}