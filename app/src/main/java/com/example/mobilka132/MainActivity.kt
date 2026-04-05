package com.example.mobilka132

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobilka132.data.location.LocationManager
import com.example.mobilka132.model.MapPoint
import com.example.mobilka132.pickBestRestaurant.DecisionDialog
import com.example.mobilka132.pickBestRestaurant.DecisionTreeManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    lateinit var mapManager: MapManager
    val viewModel: MapViewModel = MapViewModel()
    val location: LocationManager = LocationManager(this, activityResultRegistry)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mapManager = MapManager(this)

        lifecycleScope.launch {
            mapManager.loadData()
            viewModel.init(mapManager.grid)
        }

        location.checkPermission()

        setContent {
            val state = viewModel.state
            val overlay = viewModel.overlay

            val context = LocalContext.current
            val treeViewModel: DecisionTreeManager = viewModel()

            var showDecisionDialog by remember { mutableStateOf(false) }
            var showRouteMenu by remember { mutableStateOf(false) }
            var showPointsList by remember { mutableStateOf(false) }

            var startPoint by remember { mutableStateOf<Offset?>(null) }
            var endPoint by remember { mutableStateOf<Offset?>(null) }

            var startLabel by remember { mutableStateOf("Выберите начало") }
            var endLabel by remember { mutableStateOf("Выберите конец") }

            val maskBitmap = remember {
                BitmapFactory.decodeResource(context.resources, R.drawable.map)
            }

            val dummyBitmap = remember {
                BitmapFactory.decodeResource(context.resources, R.drawable.user_map_contrast)
            }

            val bitmaps = arrayOf(maskBitmap, dummyBitmap)
            var shownIndex by remember { mutableIntStateOf(0) }

            LaunchedEffect(maskBitmap) {
                state.imageSize = Size(maskBitmap.width.toFloat(), maskBitmap.height.toFloat())
            }

            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

                if (showRouteMenu) {
                    RouteMenu(
                        points = state.selectedPoints,
                        myLocation = location.mapLocation,
                        startLabel = startLabel,
                        endLabel = endLabel,
                        onStartSelected = { o, l -> startPoint = o; startLabel = l },
                        onEndSelected = { o, l -> endPoint = o; endLabel = l },
                        onClose = { showRouteMenu = false },
                        onBuildRoute = {
                            if (startPoint != null && endPoint != null) {
                                viewModel.requestPathfinding(
                                    state.findNearestAvailablePoint(startPoint!!),
                                    endPoint!!,
                                    false
                                )
                                showRouteMenu = false
                            }
                        }
                    )
                }

                Box(Modifier.padding(16.dp)) {
                    Text(
                        if (state.isProcessing) "Снаппинг..."
                        else if (state.isSelectionMode) "Выберите точку"
                        else "Интерактивная карта",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                MapContainer(
                    state,
                    bitmaps[shownIndex],
                    Modifier.weight(1f),
                    {
                        val p = state.screenToContent(it)
                        viewModel.onPointSelected(p, maskBitmap)
                    },
                    overlay,
                    viewModel,
                    location
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Row(Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly) {

                        Button({ showRouteMenu = !showRouteMenu }) {
                            Text("Маршрут")
                        }

                        Button({
                            treeViewModel.reset()
                            showDecisionDialog = true
                        }) {
                            Text("💡 Совет")
                        }
                    }

                    Row(Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly) {

                        Button({
                            state.isSelectionMode = !state.isSelectionMode
                        }) {
                            Text(if (state.isSelectionMode) "Отмена" else "Выбрать")
                        }

                        Button({ viewModel.clear() }) {
                            Text("Очистить")
                        }
                    }

                    Row(Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly) {

                        Button({
                            if (viewModel.isPathProcessing)
                                viewModel.cancelPathfinding()
                            else viewModel.requestPathfinding(true)
                        }) {
                            Text("Путь")
                        }

                        Button({
                            shownIndex = (shownIndex + 1) % 2
                        }) {
                            Text("Слой")
                        }
                    }
                }

                if (showDecisionDialog) {
                    DecisionDialog(treeViewModel) {
                        showDecisionDialog = false
                    }
                }

                if (showPointsList) {
                    PointsListDialog(
                        state.selectedPoints,
                        { showPointsList = false },
                        { viewModel.deletePoint(it) }
                    )
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
        viewModel.currentStep?.current?.let { (x, y) -> Offset(x.toFloat(), y.toFloat()) }
    }
    val nodeOffsets = remember(viewModel.currentStep) {
        viewModel.currentStep?.openSet?.map { (x, y) -> Offset(x.toFloat(), y.toFloat()) } ?: emptyList()
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

                location.mapLocation?.let {
                    drawPointUnscaled(it, 7f, Color.Yellow)
                }

                if (viewModel.currentStep != null) {

                    if (nodeOffsets.isNotEmpty()) {
                        drawPointsUnscaled(nodeOffsets, 3f, Color.Green)
                    }

                    stepOffset?.let {
                        drawPointUnscaled(it, 5f, Color.Yellow)
                    }
                }
            }
        }
        FilledIconButton(
            onClick = { location.requestNewLocationData() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (location.mapLocation != null) Color.Gray else Color.Blue,
                contentColor = Color.White
            )
        ) {
            Text("GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        val textStyle = TextStyle(
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            state.selectedPoints.forEach { point : MapPoint ->
                val screenPos = state.contentToScreen(point.position)
                val label = point.id.toString()

                drawCircle(color = Color.Red, radius = 20f, center = screenPos)
                drawCircle(color = Color.White, radius = 8f, center = screenPos)

                val textLayoutResult = textMeasurer.measure(
                    text = label,
                    style = textStyle
                )

                val textWidth = textLayoutResult.size.width.toFloat()
                val textHeight = textLayoutResult.size.height.toFloat()

                val pinWidth = maxOf(50f, textWidth + 20f)
                val pinHeight = 70f
                val tailHeight = 25f
                val pinBottomOffset = 25f

                val path = Path().apply {
                    moveTo(screenPos.x, screenPos.y - pinBottomOffset)
                    lineTo(
                        screenPos.x - pinWidth / 2,
                        screenPos.y - pinBottomOffset - tailHeight
                    )
                    lineTo(
                        screenPos.x - pinWidth / 2,
                        screenPos.y - pinBottomOffset - pinHeight
                    )
                    lineTo(
                        screenPos.x + pinWidth / 2,
                        screenPos.y - pinBottomOffset - pinHeight
                    )
                    lineTo(
                        screenPos.x + pinWidth / 2,
                        screenPos.y - pinBottomOffset - tailHeight
                    )
                    close()
                }

                drawPath(path = path, color = Color.Yellow)
                drawPath(path = path, color = Color.Black, style = Stroke(width = 2f))

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = screenPos.x - textWidth / 2,
                        y = screenPos.y - pinBottomOffset - pinHeight +
                                (pinHeight - tailHeight) / 2 - textHeight / 2
                    )
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

@Composable
fun RouteMenu(
    points: List<MapPoint>,
    myLocation: Offset?,
    onStartSelected: (Offset, String) -> Unit,
    onEndSelected: (Offset, String) -> Unit,
    onBuildRoute: () -> Unit,
    onClose: () -> Unit,
    startLabel: String,
    endLabel: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Построение маршрута", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))

            PointSelector("Откуда: $startLabel", points, myLocation, onStartSelected)

            Spacer(modifier = Modifier.height(4.dp))

            PointSelector("Куда: $endLabel", points, null, onEndSelected)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onClose) { Text("Закрыть") }
                Button(
                    onClick = onBuildRoute,
                    enabled = startLabel != "Выберите начало" && endLabel != "Выберите конец"
                ) {
                    Text("Построить")
                }
            }
        }
    }
}

@Composable
fun PointSelector(
    label: String,
    points: List<MapPoint>,
    myLocation: Offset?,
    onSelected: (Offset, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (myLocation != null) {
                DropdownMenuItem(
                    text = { Text("Моя локация") },
                    onClick = {
                        onSelected(myLocation, "Моя локация")
                        expanded = false
                    }
                )
            }
            points.forEach { point ->
                DropdownMenuItem(
                    text = { Text("Точка №${point.id}") },
                    onClick = {
                        onSelected(point.position, "Точка №${point.id}")
                        expanded = false
                    }
                )
            }
        }
    }
}