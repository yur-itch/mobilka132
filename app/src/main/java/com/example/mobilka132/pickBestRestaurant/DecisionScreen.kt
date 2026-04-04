package com.example.mobilka132.pickBestRestaurant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Подбор заведения",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                if (viewModel.recommendation.isNotEmpty()) {
                    ResultView(viewModel.recommendation, onRestart = { viewModel.reset() })
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
                            ResultView(node.result, onRestart = { viewModel.reset() })
                        }
                        else -> {
                            Text("Загрузка дерева...")
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
                val displayAnswer = when(answer) {
                    "main_building" -> "Главный корпус"
                    "second_building" -> "Второй корпус"
                    "campus_center" -> "Центр кампуса"
                    "short" -> "Мало (15-20 мин)"
                    "medium" -> "Средне (30-40 мин)"
                    "very_short" -> "Очень мало (5 мин)"
                    "long" -> "Много времени"
                    "full_meal" -> "Полноценный обед"
                    "snack" -> "Перекус"
                    "coffee" -> "Просто кофе"
                    "pancakes" -> "Блинчики"
                    "low" -> "Не хочу ждать (очереди)"
                    "high" -> "Готов подождать"
                    "good" -> "Хорошая (солнце)"
                    "bad" -> "Плохая (дождь/снег)"
                    else -> answer
                }
                
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
fun ResultView(result: String, onRestart: () -> Unit) {
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
    
    Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
        Text("Попробовать снова")
    }
}
