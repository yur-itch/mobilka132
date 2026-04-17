package com.example.mobilka132.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilka132.MapState
import com.example.mobilka132.MapViewModel
import com.example.mobilka132.R

@Composable
fun HeaderCard(
    state: MapState,
    viewModel: MapViewModel,
    onMenuClick: () -> Unit,
    onThemeClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
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
                    viewModel.isGARunning -> stringResource(
                        R.string.algo_ga_running,
                        viewModel.currentGeneration
                    )

                    viewModel.isPathProcessing -> stringResource(R.string.searching_path)
                    else -> stringResource(R.string.map_route_title)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = onThemeClick) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = "Theme",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (state.isProcessing || viewModel.isAnyAlgoRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
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
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) selectedColor else contentColor.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
    }
}
