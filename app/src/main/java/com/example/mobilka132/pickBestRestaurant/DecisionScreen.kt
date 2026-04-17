package com.example.mobilka132.pickBestRestaurant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobilka132.R

@Composable
fun DecisionDialog(
    viewModel: DecisionTreeManager,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            viewModel.isVisualizerMode -> stringResource(R.string.tree_structure)
                            viewModel.isSettingsMode -> stringResource(R.string.tree_settings)
                            else -> stringResource(R.string.tree_select_establishment)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Row {
                        IconButton(onClick = {
                            viewModel.isVisualizerMode = !viewModel.isVisualizerMode
                            viewModel.isSettingsMode = false
                        }) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = "Visualize",
                                tint = if (viewModel.isVisualizerMode) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }

                        IconButton(onClick = {
                            viewModel.isSettingsMode = !viewModel.isSettingsMode
                            viewModel.isVisualizerMode = false
                        }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (viewModel.isSettingsMode) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        viewModel.isVisualizerMode -> {
                            TreeVisualizerScreen(viewModel.rootNode)
                        }

                        viewModel.isSettingsMode -> {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                SettingsView(
                                    csvText = viewModel.userCsvText,
                                    onCsvChange = { viewModel.userCsvText = it },
                                    onApply = { viewModel.reset() }
                                )
                            }
                        }

                        viewModel.recommendation.isNotEmpty() -> {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                ResultView(
                                    result = viewModel.recommendation,
                                    path = viewModel.decisionPath,
                                    onRestart = { viewModel.reset() }
                                )
                            }
                        }

                        else -> {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                when (val node = viewModel.currentNode) {
                                    is DecisionTree.InternalNode -> {
                                        QuestionView(
                                            node = node,
                                            budgetInput = viewModel.budgetInput,
                                            onBudgetChange = { viewModel.budgetInput = it },
                                            onBudgetSubmit = { viewModel.onBudgetSubmitted() },
                                            onAnswerSelect = { viewModel.onAnswerSelected(it) },
                                            onQuickPrediction = { viewModel.stopAndGetCurrentPrediction() }
                                        )
                                    }

                                    is DecisionTree.Leaf -> {
                                        ResultView(
                                            node.result,
                                            viewModel.decisionPath,
                                            onRestart = { viewModel.reset() })
                                    }

                                    else -> {
                                        Text(stringResource(R.string.tree_not_ready))
                                    }
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.dialog_close))
                }
            }
        }
    }
}

@Composable
fun SettingsView(
    csvText: String,
    onCsvChange: (String) -> Unit,
    onApply: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.tree_csv_label),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = csvText,
            onValueChange = onCsvChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp),
            placeholder = { Text("header1,header2,target...") },
            textStyle = MaterialTheme.typography.bodySmall
        )
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.tree_apply))
        }
    }
}

@Composable
fun QuestionView(
    node: DecisionTree.InternalNode,
    budgetInput: String,
    onBudgetChange: (String) -> Unit,
    onBudgetSubmit: () -> Unit,
    onAnswerSelect: (String) -> Unit,
    onQuickPrediction: () -> Unit
) {
    val questionTitle = when (node.problemName) {
        "budget" -> stringResource(R.string.tree_question_budget)
        "location" -> stringResource(R.string.tree_question_location)
        "time_available" -> stringResource(R.string.tree_question_time)
        "food_type" -> stringResource(R.string.tree_question_food)
        "queue_tolerance" -> stringResource(R.string.tree_question_queue)
        "weather" -> stringResource(R.string.tree_question_weather)
        else -> "Question: ${node.problemName}"
    }

    Text(text = questionTitle, style = MaterialTheme.typography.titleMedium)

    if (node.problemName == "budget") {
        OutlinedTextField(
            value = budgetInput,
            onValueChange = onBudgetChange,
            label = { Text(stringResource(R.string.tree_budget_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = onBudgetSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = budgetInput.isNotEmpty()
        ) {
            Text(stringResource(R.string.tree_next))
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            node.branches.keys.forEach { answer ->
                OutlinedButton(
                    onClick = { onAnswerSelect(answer) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(answer)
                }
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    TextButton(onClick = onQuickPrediction) {
        Text(stringResource(R.string.tree_skip_and_advice), fontSize = 12.sp)
    }
}

@Composable
fun ResultView(result: String, path: List<Pair<String, String>>, onRestart: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.tree_recommendation),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = result,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (path.isNotEmpty()) {
            Text(
                stringResource(R.string.tree_decision_path),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                path.forEachIndexed { index, pair ->
                    Text(
                        "${index + 1}. ${pair.first} -> ${pair.second}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.tree_reset))
        }
    }
}
