package com.example.mobilka132

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobilka132.pickBestRestaurant.DecisionTreeManager
import com.example.mobilka132.pickBestRestaurant.DecisionDialog
import com.example.mobilka132.data.location.LocationManager
import com.example.mobilka132.model.MapPoint
import com.example.mobilka132.ui.theme.Mobilka132Theme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var mapManager: MapManager
    private val viewModel: MapViewModel by viewModels<MapViewModel>()
    private val location: LocationManager by lazy { LocationManager(this, activityResultRegistry) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mapManager = MapManager(this)

        lifecycleScope.launch {
            mapManager.loadData().await()
            viewModel.init(mapManager)
            viewModel.loadPointsFromAssets(this@MainActivity)
        }
        location.checkPermission()
        location.mapState = viewModel.state

        setContent {
            Mobilka132Theme {
                MapScreen(viewModel, location)
            }
        }
    }
}

@Composable
fun MapScreen(viewModel: MapViewModel, location: LocationManager) {
    val state = viewModel.state
    val overlay = viewModel.overlay
    val context = LocalContext.current
    val treeViewModel: DecisionTreeManager = viewModel()

    var showDecisionDialog by remember { mutableStateOf(false) }
    var showPointsList by remember { mutableStateOf(false) }
    var showRouteMenu by remember { mutableStateOf(false) }
    var showAlgoMenu by remember { mutableStateOf(false) }

    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var endPoint by remember { mutableStateOf<Offset?>(null) }
    var startLabel by remember { mutableStateOf("Откуда") }
    var endLabel by remember { mutableStateOf("Куда") }
    var visualizeRoute by remember { mutableStateOf(false) }
    var stepDelay by remember { mutableLongStateOf(5L) }

    val roadMask = remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(context.resources, R.drawable.map750, options)
    }

    val buildingsMask = remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(context.resources, R.drawable.perfect_colored_map750, options)
    }

    val dummyBitmap = remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(context.resources, R.drawable.user_map_contrast, options)
    }

    val bitmaps = arrayOf(dummyBitmap, roadMask, buildingsMask)
    var shownIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(roadMask) {
        state.imageSize = Size(roadMask.width.toFloat(), roadMask.height.toFloat())
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        MapContainer(
            state = state,
            bitmap = bitmaps[shownIndex],
            modifier = Modifier.fillMaxSize(),
            onPointSelected = { pressOffset ->
                val currentRoadMask = if (state.isSelectionMode) roadMask else null
                viewModel.onPointSelected(pressOffset, currentRoadMask, buildingsMask)
            },
            overlay = overlay,
            viewModel = viewModel,
            location = location
        )

        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterEnd),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF3C4043).copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                    .clickable { shownIndex = (shownIndex + 1) % bitmaps.size }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Вид", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            
            if (viewModel.isAnyAlgoRunning) {
                FloatingActionButton(
                    onClick = { viewModel.cancelAll() },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Стоп")
                }
            }
        }

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeaderCard(state, viewModel, onMenuClick = { showAlgoMenu = true })
            
            AnimatedVisibility(
                visible = showRouteMenu,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                RouteMenuCard(
                    points = state.selectedPoints,
                    myLocation = location.mapLocation,
                    startLabel = startLabel,
                    endLabel = endLabel,
                    isVisualized = visualizeRoute,
                    stepDelay = stepDelay,
                    onStepDelayChange = { stepDelay = it },
                    onVisualizationToggle = { visualizeRoute = it },
                    onStartSelected = { offset, label -> startPoint = offset; startLabel = label },
                    onEndSelected = { offset, label -> endPoint = offset; endLabel = label },
                    onClose = { showRouteMenu = false },
                    onBuildRoute = {
                        if (startPoint != null && endPoint != null) {
                            viewModel.requestPathfinding(
                                state.findNearestAvailablePoint(startPoint!!),
                                endPoint!!,
                                visualizeRoute,
                                stepDelay
                            )
                            showRouteMenu = false
                        }
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = state.selectedBuildingInfo != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    state.selectedBuildingInfo?.let { info ->
                        BuildingInfoCard(
                            info = info,
                            onDismiss = { state.selectedBuildingInfo = null },
                            onRouteTo = {
                                state.selectedBuildingInfo = null
                                showRouteMenu = true
                            }
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFF1A1C1E),
                    contentColor = Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                            .wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        val isBusy = viewModel.isAnyAlgoRunning || state.isProcessing

                        ControlIconButton(
                            icon = Icons.Default.Add,
                            label = "Точка",
                            isSelected = state.isSelectionMode,
                            enabled = !isBusy,
                            onClick = { state.isSelectionMode = !state.isSelectionMode },
                            selectedColor = Color(0xFF91C1F3),
                            contentColor = Color.White
                        )

                        ControlIconButton(
                            icon = Icons.Default.Route,
                            label = "Путь",
                            enabled = !isBusy,
                            onClick = { showRouteMenu = !showRouteMenu },
                            contentColor = Color.White
                        )

                        ControlIconButton(
                            icon = Icons.AutoMirrored.Filled.List,
                            label = "${state.selectedPoints.size}",
                            enabled = !isBusy,
                            onClick = { showPointsList = true },
                            contentColor = Color.White
                        )

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF1B72C0), CircleShape)
                                .clickable { location.requestNewLocationData() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NearMe, contentDescription = "GPS", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        if (showPointsList) {
            PointsListDialog(
                points = state.selectedPoints,
                onDismiss = { showPointsList = false },
                onDeletePoint = { index -> viewModel.deletePoint(index) },
                onDeleteAll = { viewModel.clear() }
            )
        }

        if (showDecisionDialog) {
            DecisionDialog(
                viewModel = treeViewModel,
                onDismiss = { showDecisionDialog = false }
            )
        }

        if (showAlgoMenu) {
            AlgoDrawer(
                onDismiss = { showAlgoMenu = false },
                onStartGA = { viewModel.startFoodShoppingGA(buildingsMask); showAlgoMenu = false },
                onStartTSP = { viewModel.findTSPSolution(); showAlgoMenu = false },
                onShowAdvice = { showDecisionDialog = true },
                isBusy = viewModel.isAnyAlgoRunning || state.isProcessing
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgoDrawer(onDismiss: () -> Unit, onStartGA: () -> Unit, onStartTSP: () -> Unit, onShowAdvice: () -> Unit, isBusy: Boolean) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Меню", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            ListItem(
                headlineContent = { Text("Совет (Дерево решений)") },
                supportingContent = { Text("Помощь в выборе места для перекуса") },
                leadingContent = { Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFFFC107)) },
                modifier = Modifier.clickable { onShowAdvice(); onDismiss() }
            )
            
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Генетический алгоритм (GA)") },
                supportingContent = { Text("Поиск оптимального маршрута для покупок") },
                leadingContent = { Icon(Icons.Default.AutoFixHigh, null, tint = Color(0xFF1B72C0)) },
                modifier = Modifier.clickable(enabled = !isBusy) { onStartGA() }
            )
            
            ListItem(
                headlineContent = { Text("Муравьиный алгоритм (TSP)") },
                supportingContent = { Text("Решение задачи коммивояжера") },
                leadingContent = { Icon(Icons.Default.TravelExplore, null, tint = Color(0xFF1B72C0)) },
                modifier = Modifier.clickable(enabled = !isBusy) { onStartTSP() }
            )
        }
    }
}

@Composable
fun BuildingInfoCard(info: BuildingInfo, onDismiss: () -> Unit, onRouteTo: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.name.ifEmpty { "Здание ТГУ" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = info.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (info.venues.isNotEmpty()) {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(info.venues) { venue ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("${venue.name} (${venue.workingHours})", fontSize = 14.sp)
                        }
                    }
                }
            } else {
                Text("Учебный корпус ТГУ", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRouteTo,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Directions, null)
                Spacer(Modifier.width(8.dp))
                Text("Проложить маршрут")
            }
        }
    }
}

@Composable
fun HeaderCard(state: MapState, viewModel: MapViewModel, onMenuClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1A1C1E).copy(alpha = 0.9f),
        contentColor = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "Меню", tint = Color.White)
            }
            
            Text(
                text = when {
                    state.isProcessing -> "Снаппинг..."
                    state.isSelectionMode -> "Выберите точку"
                    viewModel.isGARunning -> "Генетика: поколение ${viewModel.currentGeneration}"
                    viewModel.isPathProcessing -> "Поиск пути..."
                    else -> "Карта маршрутов"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = Color.White
            )

            if (state.isProcessing || viewModel.isAnyAlgoRunning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            }
        }
    }
}

@Composable
fun ControlIconButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    selectedColor: Color = Color(0xFF91C1F3),
    contentColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (isSelected) selectedColor.copy(alpha = 0.2f) else Color.Transparent,
                contentColor = if (isSelected) selectedColor else contentColor
            )
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) selectedColor else contentColor.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

@Composable
fun RouteMenuCard(
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Маршрут", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            PointSelectorRow("От", startLabel, points, myLocation, onStartSelected)
            Spacer(modifier = Modifier.height(8.dp))
            PointSelectorRow("До", endLabel, points, null, onEndSelected)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Checkbox(checked = isVisualized, onCheckedChange = onVisualizationToggle)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Визуализация A*", style = MaterialTheme.typography.bodyMedium)
                    Text("Задержка: $stepDelay мс", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            
            if (isVisualized) {
                Slider(
                    value = stepDelay.toFloat(),
                    onValueChange = { onStepDelayChange(it.toLong()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
            }

            Button(
                onClick = onBuildRoute,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                enabled = startLabel != "Откуда" && endLabel != "Куда",
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Построить маршрут")
            }
        }
    }
}

@Composable
fun PointSelectorRow(prefix: String, label: String, points: List<MapPoint>, myLocation: Offset?, onSelected: (Offset, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(prefix, modifier = Modifier.width(40.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(label, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                myLocation?.let {
                    DropdownMenuItem(
                        text = { Text("Моя локация (GPS)") },
                        leadingIcon = { Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { onSelected(it, "Моя локация"); expanded = false }
                    )
                }
                points.forEach { point ->
                    DropdownMenuItem(
                        text = { Text("Точка №${point.id}") },
                        onClick = { onSelected(point.position, "Точка №${point.id}"); expanded = false }
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
    val paths = remember(viewModel.foundPaths.size) { viewModel.foundPaths.map { overlay.generatePath(it.steps) } }
    val stepOffset = remember(viewModel.currentStep) {
        viewModel.currentStep?.current?.let { (x, y) -> Offset(x.toFloat(), y.toFloat()) }
    }
    val nodeOffsets = remember(viewModel.currentStep) {
        viewModel.currentStep?.openSet?.map { (x, y) -> Offset(x.toFloat(), y.toFloat()) } ?: emptyList()
    }
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { state.containerSize = it }
            .pointerInput(state.isProcessing) {
                detectTapGestures { if (!state.isProcessing) onPointSelected(it) }
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    state.updateTransform(centroid, pan, zoom)
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
                with(overlay) { 
                    for (path in paths) drawPathScaled(path, color = primaryColor, thickness = 6f)
                    viewModel.tspPath?.steps?.let { drawPathScaled(generatePath(it), color = Color(0xFF4CAF50)) }
                    viewModel.currentGAStep?.path?.steps?.let { drawPathScaled(generatePath(it), color = Color(0xFFFF9800)) }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            with(overlay) {
                location.mapLocation?.let { drawPointUnscaled(it, 8f, primaryColor) }
                if (viewModel.currentStep != null) {
                    if (nodeOffsets.isNotEmpty()) drawPointsUnscaled(nodeOffsets, 3f, Color.Green.copy(alpha = 0.5f))
                    stepOffset?.let { drawPointUnscaled(it, 5f, Color.Yellow) }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            state.selectedPoints.forEach { point ->
                val screenPos = state.contentToScreen(point.position)
                drawCircle(color = primaryColor, radius = 15f, center = screenPos)
                drawCircle(color = Color.White, radius = 6f, center = screenPos)

                val textLayoutResult = textMeasurer.measure(point.id.toString(), textStyle)
                val textWidth = textLayoutResult.size.width.toFloat()
                val textHeight = textLayoutResult.size.height.toFloat()
                
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(screenPos.x - textWidth/2 - 8f, screenPos.y - 45f),
                    size = Size(textWidth + 16f, textHeight + 8f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawText(textLayoutResult = textLayoutResult, color = Color.White, topLeft = Offset(screenPos.x - textWidth/2, screenPos.y - 41f))
            }
        }
    }
}

@Composable
fun PointsListDialog(points: List<MapPoint>, onDismiss: () -> Unit, onDeletePoint: (Int) -> Unit, onDeleteAll: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Мои локации", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (points.isNotEmpty()) {
                        TextButton(onClick = onDeleteAll) {
                            Text("Удалить всё", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (points.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Список пуст", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        itemsIndexed(points) { index, point ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${point.id}", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "Точка №${point.id}", style = MaterialTheme.typography.bodyLarge)
                                }
                                IconButton(onClick = { onDeletePoint(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            if (index < points.size - 1) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}