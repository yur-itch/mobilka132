package com.example.mobilka132.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobilka132.CampusDatabase
import com.example.mobilka132.MapViewModel
import com.example.mobilka132.R
import com.example.mobilka132.model.BuildingInfo
import com.example.mobilka132.model.BuildingType
import com.example.mobilka132.model.MapPoint
import com.example.mobilka132.model.ObstacleLine
import com.example.mobilka132.ui.theme.ThemeMode

@Composable
fun ThemeSelectionDialog(onDismiss: () -> Unit, onThemeChange: (ThemeMode?, Color?) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.theme_selection_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { onThemeChange(ThemeMode.LIGHT, null); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(stringResource(R.string.theme_light_tsu))
                }

                Button(
                    onClick = { onThemeChange(ThemeMode.DARK, null); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(stringResource(R.string.theme_dark))
                }

                HorizontalDivider()
                
                Text(
                    stringResource(R.string.theme_custom_color),
                    style = MaterialTheme.typography.titleMedium
                )

                val colors = listOf(
                    Color(0xFFE91E63),
                    Color(0xFF9C27B0),
                    Color(0xFF2196F3),
                    Color(0xFF4CAF50),
                    Color(0xFFFF9800),
                    Color(0xFF795548)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(color, CircleShape)
                                .clickable {
                                    onThemeChange(null, color)
                                    onDismiss()
                                }
                        )
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        }
    }
}

@Composable
fun PointsListDialog(
    points: List<MapPoint>,
    onDismiss: () -> Unit,
    onDeletePoint: (Int) -> Unit,
    onDeleteAll: () -> Unit
) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.dialog_my_locations),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (points.isNotEmpty()) {
                        TextButton(onClick = onDeleteAll) {
                            Text(
                                stringResource(R.string.dialog_delete_all),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (points.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.dialog_empty_list),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        itemsIndexed(points) { index, point ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
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
                                            Text(
                                                "${point.id}",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.point_prefix, point.id),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                IconButton(onClick = { onDeletePoint(index) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.dialog_delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (index < points.size - 1) HorizontalDivider(
                                modifier = Modifier.padding(
                                    vertical = 4.dp
                                )
                            )
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
                        Text(
                            stringResource(R.string.dialog_obstacle_line, line.id),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDelete(line.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClearAll) {
                Text(
                    stringResource(R.string.dialog_clear_all),
                    color = Color.Red
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_close)) }
        }
    )
}

@Composable
fun VenueSelectionDialog(viewModel: MapViewModel, onDismiss: () -> Unit) {
    val buildings =
        remember { CampusDatabase.getAllBuildings().filter { it.value.venues.isNotEmpty() } }

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
                    stringResource(R.string.ga_points_setup),
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
                    Text(stringResource(R.string.btn_done))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishSelectionDialog(
    viewModel: MapViewModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val allDishes = remember {
        CampusDatabase.getAllBuildings().values
            .flatMap { it.venues }
            .flatMap { it.dishes }
            .distinct()
            .sorted()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    stringResource(R.string.dish_selection_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allDishes) { dish ->
                        val isSelected = viewModel.selectedDishes.contains(dish)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleDish(dish) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleDish(dish) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = dish,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_close))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = viewModel.selectedDishes.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_go))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TspBuildingSelectionDialog(
    viewModel: MapViewModel,
    myLocation: Offset?,
    points: List<MapPoint>,
    onDismiss: () -> Unit,
    onConfirm: (Offset?) -> Unit
) {
    var selectedType by remember { mutableStateOf<BuildingType?>(null) }
    var startPointMode by remember { mutableIntStateOf(if (myLocation != null) 1 else 2) }

    var chosenPointOffset by remember { mutableStateOf<Offset?>(null) }
    val selectPointHint = stringResource(R.string.tsp_select_point_hint)
    var chosenPointLabel by remember { mutableStateOf(selectPointHint) }

    val buildings = remember { CampusDatabase.getAllBuildings() }
    val filteredBuildings = remember(selectedType) {
        if (selectedType == null) buildings
        else buildings.filter { it.value.type == selectedType }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    stringResource(R.string.tsp_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.tsp_start_point), style = MaterialTheme.typography.labelMedium)
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (myLocation != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { startPointMode = 1 }) {
                            RadioButton(selected = startPointMode == 1, onClick = { startPointMode = 1 })
                            Text(stringResource(R.string.tsp_my_location), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { startPointMode = 2 }) {
                        RadioButton(selected = startPointMode == 2, onClick = { startPointMode = 2 })
                        Text(stringResource(R.string.tsp_placed_point), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (startPointMode == 2) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PointSelectorRow(
                        prefix = stringResource(R.string.tsp_start_prefix),
                        label = chosenPointLabel,
                        points = points,
                        myLocation = null,
                        onSelected = { offset, label ->
                            chosenPointOffset = offset
                            chosenPointLabel = label
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(stringResource(R.string.tsp_filter_buildings), style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text(stringResource(R.string.tsp_filter_all)) }
                    )
                    BuildingType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.selectAllTspBuildings(filteredBuildings.keys) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.SelectAll, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.tsp_select_all), fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { viewModel.clearTspBuildings(filteredBuildings.keys) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Deselect, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.tsp_clear), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    filteredBuildings.forEach { (color, building) ->
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleTspBuilding(color) }
                                    .padding(vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = viewModel.selectedTspBuildings.contains(color),
                                    onCheckedChange = { viewModel.toggleTspBuilding(color) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = building.name.ifEmpty { building.address },
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = building.type.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val startOffset = when(startPointMode) {
                                1 -> myLocation
                                2 -> chosenPointOffset
                                else -> null
                            }
                            onConfirm(startOffset)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = viewModel.selectedTspBuildings.size >= 1 && (startPointMode != 2 || chosenPointOffset != null)
                    ) {
                        Text(stringResource(R.string.tsp_solve))
                    }
                }
            }
        }
    }
}

@Composable
fun SimulationStartDialog(
    myLocation: Offset?,
    points: List<MapPoint>,
    onDismiss: () -> Unit,
    onConfirm: (Offset?) -> Unit
) {
    var startPointMode by remember { mutableIntStateOf(if (myLocation != null) 1 else 0) }
    var chosenPointOffset by remember { mutableStateOf<Offset?>(null) }
    val selectPointHint = stringResource(R.string.simulation_select_point_hint)
    var chosenPointLabel by remember { mutableStateOf(selectPointHint) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.simulation_start_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.simulation_start_point_label), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { startPointMode = 0 }
                ) {
                    RadioButton(selected = startPointMode == 0, onClick = { startPointMode = 0 })
                    Text(stringResource(R.string.simulation_random_point), style = MaterialTheme.typography.bodyMedium)
                }
                if (myLocation != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { startPointMode = 1 }
                    ) {
                        RadioButton(selected = startPointMode == 1, onClick = { startPointMode = 1 })
                        Text(stringResource(R.string.simulation_my_location), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { startPointMode = 2 }
                ) {
                    RadioButton(selected = startPointMode == 2, onClick = { startPointMode = 2 })
                    Text(stringResource(R.string.simulation_placed_point), style = MaterialTheme.typography.bodyMedium)
                }
                if (startPointMode == 2) {
                    Spacer(Modifier.height(4.dp))
                    PointSelectorRow(
                        prefix = stringResource(R.string.simulation_start_prefix),
                        label = chosenPointLabel,
                        points = points,
                        myLocation = null,
                        onSelected = { offset, label ->
                            chosenPointOffset = offset
                            chosenPointLabel = label
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startOffset = when (startPointMode) {
                        1 -> myLocation
                        2 -> chosenPointOffset
                        else -> null
                    }
                    onConfirm(startOffset)
                    onDismiss()
                },
                enabled = startPointMode != 2 || chosenPointOffset != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.simulation_start_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}
