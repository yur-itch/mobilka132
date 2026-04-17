package com.example.mobilka132.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilka132.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgoDrawer(
    onDismiss: () -> Unit,
    onStartGA: () -> Unit,
    onStartTSP: () -> Unit,
    onStartSimulation: () -> Unit,
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
            Text(
                stringResource(R.string.menu_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

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
                leadingContent = {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
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
                leadingContent = {
                    Icon(
                        Icons.Default.TravelExplore,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable(enabled = !isBusy) { onStartTSP() }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.algo_simulation_title)) },
                supportingContent = { Text(stringResource(R.string.algo_simulation_desc)) },
                leadingContent = {
                    Icon(
                        Icons.Default.Groups,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable(enabled = !isBusy) { onStartSimulation() }
            )

            HorizontalDivider()

            Text(
                stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageButton(
                    label = stringResource(R.string.lang_ru),
                    onClick = { onLanguageChange("ru") },
                    modifier = Modifier.weight(1f)
                )
                LanguageButton(
                    label = stringResource(R.string.lang_en),
                    onClick = { onLanguageChange("en") },
                    modifier = Modifier.weight(1f)
                )
                LanguageButton(
                    label = stringResource(R.string.lang_zh),
                    onClick = { onLanguageChange("zh") },
                    modifier = Modifier.weight(1f)
                )
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
