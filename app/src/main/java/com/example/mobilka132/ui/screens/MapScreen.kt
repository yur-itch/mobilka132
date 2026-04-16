package com.example.mobilka132.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobilka132.*
import com.example.mobilka132.R
import com.example.mobilka132.data.location.LocationManager
import com.example.mobilka132.model.*
import com.example.mobilka132.pickBestRestaurant.DecisionDialog
import com.example.mobilka132.pickBestRestaurant.DecisionTreeManager
import com.example.mobilka132.ui.components.*
import com.example.mobilka132.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
