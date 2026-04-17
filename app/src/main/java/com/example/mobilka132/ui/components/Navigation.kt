package com.example.mobilka132.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
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
    onShowAdvice: () -> Unit,
    onConfigureGA: () -> Unit,
    onLanguageChange: (String) -> Unit,
    isBusy: Boolean
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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

            TBankAdBanner()
        }
    }
}

@Composable
fun TBankAdBanner(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFDD2D),
        contentColor = Color.Black
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.Black, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "T",
                    color = Color(0xFFFFDD2D),
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Т-Банк",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "Лучший мобильный банк",
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }

            Button(
                onClick = { uriHandler.openUri("https://www.tbank.ru") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Открыть", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
