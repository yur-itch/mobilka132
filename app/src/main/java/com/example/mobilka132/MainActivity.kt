package com.example.mobilka132

import android.content.Context
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobilka132.pickBestRestaurant.DecisionTreeManager
import com.example.mobilka132.pickBestRestaurant.DecisionDialog
import com.example.mobilka132.data.location.LocationManager
import com.example.mobilka132.model.*
import com.example.mobilka132.ui.theme.Mobilka132Theme
import com.example.mobilka132.ui.theme.ThemeMode
import com.example.mobilka132.data.LocaleHelper
import com.example.mobilka132.data.ThemeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var mapManager: MapManager
    private val viewModel: MapViewModel by viewModels<MapViewModel>()
    private val location: LocationManager by lazy { LocationManager(this, activityResultRegistry) }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mapManager = MapManager(this)

        lifecycleScope.launch {
            mapManager.loadData().await()
            viewModel.init(mapManager)
            viewModel.loadPointsFromAssets(this@MainActivity)
        }
        location.pixelsInMeter = viewModel.state.metersPerPixel.toFloat()

        setContent {
            var currentTheme by remember { mutableStateOf(ThemeHelper.getTheme(this)) }
            var customColor by remember { mutableStateOf(ThemeHelper.getCustomColor(this)) }

            Mobilka132Theme(themeMode = currentTheme, customColor = customColor) {
                MapScreen(
                    viewModel = viewModel,
                    location = location,
                    onLanguageChange = { lang ->
                        LocaleHelper.setLocale(this, lang)
                        recreate()
                    },
                    onThemeChange = { theme, color ->
                        ThemeHelper.setTheme(this, theme)
                        if (color != null) {
                            ThemeHelper.setCustomColor(this, color)
                            customColor = color
                        }
                        currentTheme = theme
                    }
                )
            }
        }
    }
}

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    location: LocationManager,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (ThemeMode, Color?) -> Unit
) {
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
    var showThemeMenu by remember { mutableStateOf(false) }

    val defaultFrom = stringResource(R.string.route_from_placeholder)
    val defaultTo = stringResource(R.string.route_to_placeholder)

    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var endPoint by remember { mutableStateOf<Offset?>(null) }
    var startLabel by remember { mutableStateOf("") }
    var endLabel by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (startLabel.isEmpty()) startLabel = defaultFrom
        if (endLabel.isEmpty()) endLabel = defaultTo
    }

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
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val filteredResults: List<SearchResult> = remember(searchQuery) {
        if (searchQuery.isEmpty()) emptyList()
        else {
            val q = searchQuery.lowercase()
            val results = mutableListOf<SearchResult>()
            CampusDatabase.getAllBuildings().values.forEach { building ->
                if (building.name.lowercase().contains(q)) {
                    results.add(SearchResult.BuildingResult(building, MatchType.NAME))
                } else if (building.address.lowercase().contains(q)) {
                    results.add(SearchResult.BuildingResult(building, MatchType.ADDRESS))
                }
                building.venues.forEach { venue ->
                    if (venue.name.lowercase().contains(q)) {
                        results.add(SearchResult.VenueResult(venue, building))
                    }
                }
            }
            results
        }
    }

    fun buildCentroidForBuilding(buildingInfo: BuildingInfo): Offset? {
        val colorKey = CampusDatabase.getAllBuildings().entries.find { it.value == buildingInfo }?.key ?: return null
        val w = buildingsMask.width
        val h = buildingsMask.height
        val pixels = IntArray(w * h)
        buildingsMask.getPixels(pixels, 0, w, 0, 0, w, h)
        var sumX = 0L
        var sumY = 0L
        var count = 0
        for (i in pixels.indices) {
            if ((pixels[i] and 0x00FFFFFF) == colorKey) {
                sumX += (i % w).toLong()
                sumY += (i / w).toLong()
                count++
            }
        }
        return if (count > 0) Offset(sumX.toFloat() / count, sumY.toFloat() / count) else null
    }

    val onSearchResultClick: (SearchResult) -> Unit = { result ->
        showSearch = false
        searchQuery = ""
        coroutineScope.launch(Dispatchers.Default) {
            val buildingInfo = when (result) {
                is SearchResult.BuildingResult -> result.info
                is SearchResult.VenueResult -> result.building
            }
            val centroid = buildCentroidForBuilding(buildingInfo) ?: return@launch
            val snapped = state.findNearestAvailablePoint(centroid)
            withContext(Dispatchers.Main) {
                state.addPoint(snapped)
                state.lastClickContentPoint = snapped
                when (result) {
                    is SearchResult.BuildingResult -> {
                        state.selectedVenueInfo = null
                        state.selectedBuildingInfo = result.info
                    }
                    is SearchResult.VenueResult -> {
                        state.selectedBuildingInfo = result.building
                        state.selectedVenueInfo = result.venue
                    }
                }
            }
        }
    }

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
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                    .clickable { shownIndex = (shownIndex + 1) % bitmaps.size }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Layers, contentDescription = stringResource(R.string.map_view), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            }

            if (viewModel.isAnyAlgoRunning) {
                FloatingActionButton(
                    onClick = { viewModel.cancelAll() },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.stop_algo))
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
            if (!showSearch) {
                HeaderCard(state, viewModel,
                    onMenuClick = { showAlgoMenu = true },
                    onThemeClick = { showThemeMenu = true }
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                        .clickable { showSearch = true }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Поиск", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                }
            } else {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    results = filteredResults,
                    onResultClick = onSearchResultClick,
                    onClose = { showSearch = false; searchQuery = "" }
                )
            }

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
                .fillMaxSize()
                .padding(bottom = 170.dp, end = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = {
                    location.checkPermission()
                    location.requestNewLocationData()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.NearMe, contentDescription = stringResource(R.string.gps_label))
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
                                            endLabel = context.getString(R.string.point_prefix, newPoint.id)
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
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
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
                            label = stringResource(R.string.label_point),
                            isSelected = state.isSelectionMode,
                            enabled = !isBusy,
                            onClick = { state.isSelectionMode = !state.isSelectionMode },
                            selectedColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )

                        ControlIconButton(
                            icon = Icons.Default.Add,
                            label = stringResource(R.string.label_obstacle),
                            isSelected = viewModel.isObstacleMode,
                            enabled = !isBusy,
                            onClick = { viewModel.isObstacleMode = !viewModel.isObstacleMode },
                            selectedColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )

                        ControlIconButton(
                            icon = Icons.Default.Route,
                            label = stringResource(R.string.label_path),
                            enabled = !isBusy,
                            onClick = { showRouteMenu = !showRouteMenu },
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )

                        ControlIconButton(
                            icon = Icons.AutoMirrored.Filled.List,
                            label = "${state.selectedPoints.size}",
                            enabled = !isBusy,
                            onClick = { showPointsList = true },
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )

                        ControlIconButton(
                            icon = Icons.Default.Archive,
                            label = "${viewModel.obstacles.size}",
                            enabled = !isBusy,
                            onClick = { showObstacleMenu = true },
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (showThemeMenu) {
            ThemeSelectionDialog(
                onDismiss = { showThemeMenu = false },
                onThemeChange = onThemeChange
            )
        }

        if (showPointsList) {
            PointsListDialog(
                points = state.selectedPoints,
                onDismiss = { showPointsList = false },
                onDeletePoint = { index ->
                    val pointToDelete = state.selectedPoints.getOrNull(index)
                    viewModel.deletePoint(index)
                    if (pointToDelete != null) {
                        val prefix = context.getString(R.string.point_prefix, pointToDelete.id)
                        if (startPoint == pointToDelete.position && startLabel == prefix) {
                            startPoint = null
                            startLabel = defaultFrom
                        }
                        if (endPoint == pointToDelete.position && endLabel == prefix) {
                            endPoint = null
                            endLabel = defaultTo
                        }
                    }
                },
                onDeleteAll = {
                    viewModel.clear()
                    val sampleLabel = context.getString(R.string.point_prefix, 0)
                    val prefixPart = sampleLabel.substring(0, sampleLabel.indexOf("0").coerceAtLeast(0))
                    if (startLabel.startsWith(prefixPart)) {
                        startPoint = null
                        startLabel = defaultFrom
                    }
                    if (endLabel.startsWith(prefixPart)) {
                        endPoint = null
                        endLabel = defaultTo
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
                onLanguageChange = onLanguageChange,
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
            val thanksMsg = stringResource(R.string.feedback_thanks, "%s")
            DigitRatingDialog(
                onDismiss = { showRatingDialog = false },
                onRatingSubmitted = { rating ->
                    Toast.makeText(context, thanksMsg.format(rating), Toast.LENGTH_SHORT).show()
                    showRatingDialog = false
                }
            )
        }
    }
}

@Composable
fun ThemeSelectionDialog(onDismiss: () -> Unit, onThemeChange: (ThemeMode, Color?) -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Настройка темы", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                Button(
                    onClick = { onThemeChange(ThemeMode.LIGHT, null); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text("Светлая (ТГУ)")
                }

                Button(
                    onClick = { onThemeChange(ThemeMode.DARK, null); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text("Темная")
                }

                HorizontalDivider()
                Text("Кастомный цвет", style = MaterialTheme.typography.titleMedium)

                val colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF795548))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(color, CircleShape)
                                .clickable {
                                    onThemeChange(ThemeMode.CUSTOM, color)
                                    onDismiss()
                                }
                        )
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Закрыть")
                }
            }
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
    onLanguageChange: (String) -> Unit,
    isBusy: Boolean
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.menu_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            ListItem(
                headlineContent = { Text(stringResource(R.string.algo_advice_title)) },
                supportingContent = { Text(stringResource(R.string.algo_advice_desc)) },
                leadingContent = { Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFFFC107)) },
                modifier = Modifier.clickable { onShowAdvice(); onDismiss() }
            )

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.algo_ga_title)) },
                supportingContent = { Text(stringResource(R.string.algo_ga_desc)) },
                leadingContent = { Icon(Icons.Default.AutoFixHigh, null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = {
                    IconButton(onClick = { onConfigureGA() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настроить")
                    }
                },
                modifier = Modifier.clickable(enabled = !isBusy) { onStartGA() }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.algo_tsp_title)) },
                supportingContent = { Text(stringResource(R.string.algo_tsp_desc)) },
                leadingContent = { Icon(Icons.Default.TravelExplore, null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable(enabled = !isBusy) { onStartTSP() }
            )

            HorizontalDivider()

            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageButton(label = stringResource(R.string.lang_ru), onClick = { onLanguageChange("ru") }, modifier = Modifier.weight(1f))
                LanguageButton(label = stringResource(R.string.lang_en), onClick = { onLanguageChange("en") }, modifier = Modifier.weight(1f))
                LanguageButton(label = stringResource(R.string.lang_zh), onClick = { onLanguageChange("zh") }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun LanguageButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    Text(text = info.name.ifEmpty { stringResource(R.string.building_no_name) }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    if (info.address.isNotEmpty()) {
                        Text(text = info.address, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.dialog_close), modifier = Modifier.size(20.dp))
                }
            }

            if (info.venues.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.building_venues_title),
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
                    text = stringResource(R.string.building_no_venues),
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
                    Text(stringResource(R.string.btn_route_here))
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
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
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.dialog_close), modifier = Modifier.size(20.dp))
                }
            }

            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .heightIn(max = 250.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                InfoRow(icon = Icons.Default.Schedule, label = stringResource(R.string.venue_working_hours), value = venue.workingHours)
                InfoRow(icon = Icons.Default.Timer, label = stringResource(R.string.venue_visit_time), value = stringResource(R.string.venue_visit_time_value, venue.estimatedVisitTimeMinutes))

                if (venue.dishes.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.venue_menu_title),
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
                Text(stringResource(R.string.venue_leave_feedback))
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
fun HeaderCard(state: MapState, viewModel: MapViewModel, onMenuClick: () -> Unit, onThemeClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = stringResource(R.string.menu_title),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = when {
                    viewModel.isTSPProcessing -> stringResource(R.string.algo_tsp_running)
                    viewModel.isGARunning -> stringResource(R.string.algo_ga_running, viewModel.currentGeneration)
                    viewModel.isPathProcessing -> stringResource(R.string.searching_path)
                    else -> stringResource(R.string.map_route_title)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = onThemeClick) {
                Icon(Icons.Default.Palette, contentDescription = "Theme", tint = MaterialTheme.colorScheme.primary)
            }

            if (state.isProcessing || viewModel.isAnyAlgoRunning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
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
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
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
                Text(stringResource(R.string.route_card_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            PointSelectorRow(stringResource(R.string.route_from), startLabel, points, myLocation, onStartSelected)
            Spacer(modifier = Modifier.height(8.dp))
            PointSelectorRow(stringResource(R.string.route_to), endLabel, points, null, onEndSelected)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Checkbox(checked = isVisualized, onCheckedChange = onVisualizationToggle)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(stringResource(R.string.visualization_astar), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.delay_label, stepDelay), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
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
                enabled = startLabel != stringResource(R.string.route_from_placeholder) && endLabel != stringResource(R.string.route_to_placeholder),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.btn_build_route))
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
            val myLocationLabel = stringResource(R.string.my_location_gps)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                myLocation?.let {
                    DropdownMenuItem(
                        text = { Text(myLocationLabel) },
                        leadingIcon = { Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { onSelected(it, myLocationLabel); expanded = false }
                    )
                }
                points.forEach { point ->
                    val pointLabel = stringResource(R.string.point_prefix, point.id)
                    DropdownMenuItem(
                        text = { Text(pointLabel) },
                        onClick = { onSelected(point.position, pointLabel); expanded = false }
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
        viewModel.currentStep?.current?.let { (x, y) -> Offset(x, y) }
    }

    val nodeOffsets = remember(viewModel.currentStep) {
        viewModel.currentStep?.openSet?.map { (x, y) -> Offset(x, y) } ?: emptyList()
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
                    Text(stringResource(R.string.dialog_my_locations), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (points.isNotEmpty()) {
                        TextButton(onClick = onDeleteAll) {
                            Text(stringResource(R.string.dialog_delete_all), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (points.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.dialog_empty_list), color = MaterialTheme.colorScheme.outline)
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
                                    Text(text = stringResource(R.string.point_prefix, point.id), style = MaterialTheme.typography.bodyLarge)
                                }
                                IconButton(onClick = { onDeletePoint(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.dialog_delete), tint = MaterialTheme.colorScheme.error)
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
                    Text(stringResource(R.string.dialog_close))
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
        title = { Text(stringResource(R.string.dialog_obstacles_title)) },
        text = {
            LazyColumn {
                items(obstacles) { line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.dialog_obstacle_line, line.id), modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDelete(line.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClearAll) { Text(stringResource(R.string.dialog_clear_all), color = Color.Red) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_close)) }
        }
    )
}

fun highlightMatches(text: String, query: String, highlightColor: Color, highlightBackground: Color): AnnotatedString {
    if (query.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var cursor = 0
        while (cursor < text.length) {
            val matchAt = lowerText.indexOf(lowerQuery, cursor)
            if (matchAt == -1) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, matchAt))
            withStyle(
                SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.Bold,
                    background = highlightBackground
                )
            ) {
                append(text.substring(matchAt, matchAt + query.length))
            }
            cursor = matchAt + query.length
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<SearchResult>,
    onResultClick: (SearchResult) -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val highlightColor = MaterialTheme.colorScheme.primary
    val highlightBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Поиск зданий или заведений...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {})
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                    }
                }
            }

            if (results.isNotEmpty()) {
                HorizontalDivider()
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(results) { result ->
                        val (title, sub) = when(result) {
                            is SearchResult.BuildingResult -> result.info.name to result.info.address
                            is SearchResult.VenueResult -> result.venue.name to result.building.name
                        }
                        ListItem(
                            headlineContent = {
                                Text(highlightMatches(title, query, highlightColor, highlightBg))
                            },
                            supportingContent = {
                                Text(
                                    highlightMatches(sub, query, highlightColor, highlightBg),
                                    fontSize = 12.sp
                                )
                            },
                            leadingContent = {
                                Icon(
                                    if (result is SearchResult.BuildingResult) Icons.Default.Business else Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable { onResultClick(result) }
                        )
                    }
                }
            }
        }
    }
}
