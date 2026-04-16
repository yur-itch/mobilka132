package com.example.mobilka132

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobilka132.pickBestRestaurant.DecisionTreeManager
import com.example.mobilka132.pickBestRestaurant.DecisionDialog
import com.example.mobilka132.data.location.LocationManager
import com.example.mobilka132.model.MapPoint
import com.example.mobilka132.model.ObstacleLine
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
    val scope = rememberCoroutineScope()

    var showDecisionDialog by remember { mutableStateOf(false) }
    var showPointsList by remember { mutableStateOf(false) }
    var showRouteMenu by remember { mutableStateOf(false) }
    var showAlgoMenu by remember { mutableStateOf(false) }
    var showObstacleMenu by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showVenueSelectionDialog by remember { mutableStateOf(false) }

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
                    visible = state.selectedVenueInfo != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    state.selectedVenueInfo?.let { venue ->
                        VenueInfoCard(
                            venue = venue,
                            onDismiss = { state.selectedVenueInfo = null },
                            onBack = { state.selectedVenueInfo = null },
                            onLeaveFeedback = { showRatingDialog = true }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = state.selectedBuildingInfo != null && state.selectedVenueInfo == null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    state.selectedBuildingInfo?.let { info ->
                        BuildingInfoCard(
                            info = info,
                            onDismiss = { state.selectedBuildingInfo = null },
                            onVenueClick = { venue -> state.selectedVenueInfo = venue },
                            onRouteTo = {
                                val buildingPos = state.lastClickContentPoint
                                if (buildingPos != null) {
                                    scope.launch {
                                        val newPoint = state.addPoint(buildingPos)
                                        if (newPoint != null) {
                                            endPoint = newPoint.position
                                            endLabel = "Точка №${newPoint.id}"
                                            showRouteMenu = true
                                        }
                                    }
                                }
                                state.selectedBuildingInfo = null
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
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                            icon = Icons.Default.Add,
                            label = "Препятствие",
                            isSelected = viewModel.isObstacleMode,
                            enabled = !isBusy,
                            onClick = { viewModel.isObstacleMode = !viewModel.isObstacleMode },
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

                        ControlIconButton(
                            icon = Icons.Default.Archive,
                            label = "${viewModel.obstacles.size}",
                            enabled = !isBusy,
                            onClick = { showObstacleMenu = true },
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
                onDeletePoint = { index -> 
                    val pointToDelete = state.selectedPoints.getOrNull(index)
                    viewModel.deletePoint(index)
                    if (pointToDelete != null) {
                        if (startPoint == pointToDelete.position && startLabel == "Точка №${pointToDelete.id}") {
                            startPoint = null
                            startLabel = "Откуда"
                        }
                        if (endPoint == pointToDelete.position && endLabel == "Точка №${pointToDelete.id}") {
                            endPoint = null
                            endLabel = "Куда"
                        }
                    }
                },
                onDeleteAll = { 
                    viewModel.clear()
                    if (startLabel.startsWith("Точка №")) {
                        startPoint = null
                        startLabel = "Откуда"
                    }
                    if (endLabel.startsWith("Точка №")) {
                        endPoint = null
                        endLabel = "Куда"
                    }
                }
            )
        }

        if (showObstacleMenu) {
            ObstacleListDialog(
                obstacles = viewModel.obstacles,
                onDismiss = { showObstacleMenu = false },
                onDelete = { id ->
                    viewModel.removeObstacle(id)
                    viewModel.syncObstacles()
                },
                onClearAll = {
                    viewModel.clearObstacles()
                    viewModel.syncObstacles()
                    showObstacleMenu = false
                }
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
                onConfigureGA = { showVenueSelectionDialog = true },
                isBusy = viewModel.isAnyAlgoRunning || state.isProcessing
            )
        }

        if (showVenueSelectionDialog) {
            VenueSelectionDialog(
                viewModel = viewModel,
                onDismiss = { showVenueSelectionDialog = false }
            )
        }

        if (showRatingDialog) {
            DigitRatingDialog(
                onDismiss = { showRatingDialog = false },
                onRatingSubmitted = { rating ->
                    Toast.makeText(context, "Спасибо за оценку: $rating!", Toast.LENGTH_SHORT).show()
                    showRatingDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgoDrawer(
    onDismiss: () -> Unit,
    onStartGA: () -> Unit,
    onStartTSP: () -> Unit,
    onShowAdvice: () -> Unit,
    onConfigureGA: () -> Unit,
    isBusy: Boolean
) {
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
                trailingContent = {
                    IconButton(onClick = { onConfigureGA() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настроить")
                    }
                },
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
fun VenueSelectionDialog(viewModel: MapViewModel, onDismiss: () -> Unit) {
    val buildings = remember { CampusDatabase.getAllBuildings().filter { it.value.venues.isNotEmpty() } }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Настройка точек GA",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    buildings.forEach { (color, building) ->
                        item {
                            BuildingSelectionItem(
                                building = building,
                                color = color,
                                viewModel = viewModel
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Готово")
                }
            }
        }
    }
}

@Composable
fun BuildingSelectionItem(building: BuildingInfo, color: Int, viewModel: MapViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val selectedVenues = viewModel.selectedVenues[color] ?: emptySet()
    
    val toggleState = when {
        selectedVenues.isEmpty() -> ToggleableState.Off
        selectedVenues.size == building.venues.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp)
        ) {
            TriStateCheckbox(
                state = toggleState,
                onClick = {
                    val newState = toggleState != ToggleableState.On
                    viewModel.setBuildingVenues(color, newState)
                }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = building.name.ifEmpty { building.address },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }
        
        if (expanded) {
            Column(modifier = Modifier.padding(start = 32.dp)) {
                building.venues.forEach { venue ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleVenue(color, venue.name) }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = selectedVenues.contains(venue.name),
                            onCheckedChange = { viewModel.toggleVenue(color, venue.name) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(venue.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun BuildingInfoCard(info: BuildingInfo, onDismiss: () -> Unit, onVenueClick: (VenueInfo) -> Unit, onRouteTo: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = info.name.ifEmpty { "Здание без названия" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    if (info.address.isNotEmpty()) {
                        Text(text = info.address, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }
            
            if (info.venues.isNotEmpty()) {
                Text(
                    text = "Заведения в этом здании:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    info.venues.forEach { venue ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVenueClick(venue) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = venue.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "В этом здании нет зарегистрированных заведений",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRouteTo,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Directions, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Сюда")
                }
            }
        }
    }
}

@Composable
fun VenueInfoCard(venue: VenueInfo, onDismiss: () -> Unit, onBack: () -> Unit, onLeaveFeedback: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = venue.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }

            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .heightIn(max = 250.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                InfoRow(icon = Icons.Default.Schedule, label = "Часы работы", value = venue.workingHours)
                InfoRow(icon = Icons.Default.Timer, label = "Среднее время визита", value = "${venue.estimatedVisitTimeMinutes} мин")
                
                if (venue.dishes.isNotEmpty()) {
                    Text(
                        text = "Меню:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    venue.dishes.forEach { dish ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Text(dish, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Button(
                onClick = onLeaveFeedback,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Icon(Icons.Default.RateReview, null)
                Spacer(Modifier.width(8.dp))
                Text("Оставить отзыв (оценить)")
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun HeaderCard(state: MapState, viewModel: MapViewModel, onMenuClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1C1E).copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onMenuClick, colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)) {
                Icon(Icons.Default.Menu, contentDescription = "Меню")
            }
            
            Text(
                text = when {
                    viewModel.isTSPProcessing -> "TSP: поиск..."
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
    val scope = rememberCoroutineScope()

    var draggingLine by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    val paths = remember(viewModel.foundPaths.size) {
        viewModel.foundPaths.map { overlay.generatePath(it.steps) }
    }

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
            .pointerInput(state.isSelectionMode, state.isProcessing) {
                detectTapGestures { offset ->
                    if (!state.isProcessing && !viewModel.isObstacleMode && !viewModel.isProcessing) {
                        onPointSelected(offset)
                    }
                }
            }
            .pointerInput(viewModel.isObstacleMode) {
                if (viewModel.isObstacleMode) {
                    detectDragGestures(
                        onDragStart = { touchOffset ->
                            val contentPos = state.screenToContent(touchOffset)
                            val threshold = 25f / state.scale
                            val hit = viewModel.obstacles.firstNotNullOfOrNull { line ->
                                when {
                                    (contentPos - line.start).getDistance() < threshold -> line.id to true
                                    (contentPos - line.end).getDistance() < threshold -> line.id to false
                                    else -> null
                                }
                            }

                            if (hit != null) {
                                draggingLine = hit
                            } else {
                                val newId = (viewModel.obstacles.maxOfOrNull { it.id } ?: 0) + 1
                                val newLine = ObstacleLine(newId, contentPos, contentPos)
                                viewModel.addObstacle(newLine)
                                draggingLine = newId to false
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val contentPos = state.screenToContent(change.position)
                            draggingLine?.let { (id, isStart) ->
                                val index = viewModel.obstacles.indexOfFirst { it.id == id }
                                if (index != -1) {
                                    val line = viewModel.obstacles[index]
                                    viewModel.obstacles[index] = if (isStart) {
                                        line.copy(start = contentPos)
                                    } else {
                                        line.copy(end = contentPos)
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            draggingLine = null
                            viewModel.isObstacleMode = false
                            viewModel.syncObstacles()
                        }
                    )
                }
            }
            .pointerInput(viewModel.isObstacleMode) {
                if (!viewModel.isObstacleMode) {
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
                    for (p in paths) {
                        drawPathScaled(p)
                    }
                    viewModel.tspPath?.steps?.let { drawPathScaled(generatePath(it), color = Color(0xFF4CAF50)) }
                    viewModel.currentGAStep?.path?.steps?.let { drawPathScaled(generatePath(it), color = Color(0xFFFF9800)) }

                    withTransform({
                        translate(state.extraSpaceX, state.extraSpaceY)
                        scale(state.fitScale, state.fitScale, pivot = Offset.Zero)
                    }) {
                        viewModel.obstacles.forEach { line ->
                            drawLine(
                                color = Color.Red,
                                start = line.start,
                                end = line.end,
                                strokeWidth = 8f / state.scale,
                                cap = StrokeCap.Round
                            )
                            drawCircle(Color.Red, radius = 10f / state.scale, center = line.start)
                            drawCircle(Color.Red, radius = 10f / state.scale, center = line.end)
                        }
                    }
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

                if (screenPos.x in 0f..size.width && screenPos.y in 0f..size.height) {
                    drawCircle(color = primaryColor, radius = 15f, center = screenPos)
                    drawCircle(color = Color.White, radius = 6f, center = screenPos)

                    val textLayoutResult = textMeasurer.measure(point.id.toString(), textStyle)
                    val textWidth = textLayoutResult.size.width.toFloat()
                    val textHeight = textLayoutResult.size.height.toFloat()

                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(screenPos.x - textWidth / 2 - 8f, screenPos.y - 45f),
                        size = Size(textWidth + 16f, textHeight + 8f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        color = Color.White,
                        topLeft = Offset(screenPos.x - textWidth / 2, screenPos.y - 41f)
                    )
                }
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

@Composable
fun ObstacleListDialog(
    obstacles: List<ObstacleLine>,
    onDismiss: () -> Unit,
    onDelete: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Препятствия") },
        text = {
            LazyColumn {
                items(obstacles) { line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Линия #${line.id}", modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDelete(line.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClearAll) { Text("Очистить все", color = Color.Red) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}
