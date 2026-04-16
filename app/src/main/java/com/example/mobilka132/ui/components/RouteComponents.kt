package com.example.mobilka132.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilka132.R
import com.example.mobilka132.model.MapPoint

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
    val fromPlaceholder = stringResource(R.string.route_from_placeholder)
    val toPlaceholder = stringResource(R.string.route_to_placeholder)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.route_card_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            PointSelectorRow(
                stringResource(R.string.route_from),
                startLabel,
                points,
                myLocation,
                onStartSelected
            )
            Spacer(modifier = Modifier.height(8.dp))
            PointSelectorRow(
                stringResource(R.string.route_to),
                endLabel,
                points,
                null,
                onEndSelected
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Checkbox(checked = isVisualized, onCheckedChange = onVisualizationToggle)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        stringResource(R.string.visualization_astar),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.delay_label, stepDelay),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (isVisualized) {
                Slider(
                    value = stepDelay.toFloat(),
                    onValueChange = { onStepDelayChange(it.toLong()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Button(
                onClick = onBuildRoute,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = startLabel != fromPlaceholder && endLabel != toPlaceholder,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.btn_build_route))
            }
        }
    }
}

@Composable
fun PointSelectorRow(
    prefix: String,
    label: String,
    points: List<MapPoint>,
    myLocation: Offset?,
    onSelected: (Offset, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            prefix,
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
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
                        leadingIcon = {
                            Icon(
                                Icons.Default.MyLocation,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
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
