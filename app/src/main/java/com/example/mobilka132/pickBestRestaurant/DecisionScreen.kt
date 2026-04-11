package com.example.mobilka132.pickBestRestaurant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

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
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (viewModel.isSettingsMode) "Настройка данных" else "Подбор заведения",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.isSettingsMode = !viewModel.isSettingsMode }) {
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = "Settings", 
                            tint = if (viewModel.isSettingsMode) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }

                if (viewModel.isSettingsMode) {
                    SettingsView(
                        csvText = viewModel.userCsvText,
                        onCsvChange = { viewModel.userCsvText = it },
                        onApply = { viewModel.reset() }
                    )
                } else if (viewModel.recommendation.isNotEmpty()) {
                    ResultView(
                        result = viewModel.recommendation,
                        path = viewModel.decisionPath,
                        onRestart = { viewModel.reset() }
                    )
                } else {
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
                            ResultView(node.result, viewModel.decisionPath, onRestart = { viewModel.reset() })
                        }
                        else -> {
                            Text("Дерево пусто. Проверьте CSV в настройках.")
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Закрыть")
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
        Text("Вставьте обучающую выборку (CSV):", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = csvText,
            onValueChange = onCsvChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp, max = 300.dp),
            placeholder = { Text("location,budget,time,food,queue,weather,target...") },
            textStyle = MaterialTheme.typography.bodySmall
        )
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Обновить дерево и начать")
        }
        Text(
            "Формат: первая строка - заголовок. Далее - данные через запятую.",
            fontSize = 11.sp,
            color = Color.Gray
        )
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
    val questionTitle = when(node.problemName) {
        "budget" -> "Какой у вас бюджет?"
        "location" -> "Где вы сейчас находитесь?"
        "time_available" -> "Сколько у вас свободного времени?"
        "food_type" -> "Что именно хотите поесть?"
        "queue_tolerance" -> "Насколько вы терпимы к очередям?"
        "weather" -> "Какая сейчас погода?"
        else -> "Вопрос про ${node.problemName}:"
    }

    Text(text = questionTitle, style = MaterialTheme.typography.titleMedium)

    if (node.problemName == "budget") {
        OutlinedTextField(
            value = budgetInput,
            onValueChange = onBudgetChange,
            label = { Text("Сумма в рублях") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = onBudgetSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = budgetInput.isNotEmpty()
        ) {
            Text("Далее")
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            node.branches.keys.forEach { answer ->
                val displayAnswer = translateValue(node.problemName, answer)
                OutlinedButton(
                    onClick = { onAnswerSelect(answer) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(displayAnswer)
                }
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    
    TextButton(onClick = onQuickPrediction) {
        Text("Не хочу отвечать, дай любой совет", fontSize = 12.sp)
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
                Text("РЕКОМЕНДАЦИЯ:", fontSize = 12.sp, fontWeight = FontWeight.Light)
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
                text = "Ваш путь принятия решения:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                path.forEachIndexed { index, pair ->
                    val question = translateKey(pair.first)
                    val answer = translateValue(pair.first, pair.second)
                    Text("${index + 1}. $question -> $answer", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }
        
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text("Попробовать снова")
        }
    }
}

private fun translateKey(key: String): String {
    return when(key) {
        "budget" -> "Бюджет"
        "location" -> "Локация"
        "time_available" -> "Время"
        "food_type" -> "Тип еды"
        "queue_tolerance" -> "Очереди"
        "weather" -> "Погода"
        else -> key
    }
}

private fun translateValue(key: String, value: String): String {
    return when(key) {
        "budget" -> when(value) {
            "low" -> "Низкий"
            "medium" -> "Средний"
            "high" -> "Высокий"
            else -> value
        }
        "time_available" -> when(value) {
            "very_short" -> "Очень мало (5 мин)"
            "short" -> "Мало (15-20 мин)"
            "medium" -> "Средне (30-40 мин)"
            "long" -> "Много времени"
            else -> value
        }
        "location" -> when(value) {
            "main_building" -> "Главный корпус"
            "second_building" -> "Второй корпус"
            "campus_center" -> "Центр кампуса"
            "bus_stop" -> "Остановка"
            else -> value
        }
        "food_type" -> when(value) {
            "coffee" -> "Просто кофе"
            "pancakes" -> "Блинчики"
            "full_meal" -> "Полноценный обед"
            "snack" -> "Перекус"
            else -> value
        }
        "queue_tolerance" -> when(value) {
            "low" -> "Не хочу ждать"
            "medium" -> "Средне"
            "high" -> "Готов ждать"
            else -> value
        }
        "weather" -> when(value) {
            "good" -> "Хорошая"
            "bad" -> "Плохая"
            else -> value
        }
        else -> value
    }
}
