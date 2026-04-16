package com.example.mobilka132

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import com.example.mobilka132.data.digit_recognition.DigitRecognitionPrep
import com.example.mobilka132.data.digit_recognition.ModelLoader
import com.example.mobilka132.data.digit_recognition.NeuralNetwork

@Composable
fun DigitRatingDialog(
    onDismiss: () -> Unit,
    onRatingSubmitted: (Int) -> Unit
) {
    val context = LocalContext.current
    val nn = remember {
        val loader = ModelLoader(context)
        val l1w = loader.loadMatrix("weights/layer_1_weights.csv")
        val l1b = loader.loadVector("weights/layer_1_biases.csv")
        val l2w = loader.loadMatrix("weights/layer_2_weights.csv")
        val l2b = loader.loadVector("weights/layer_2_biases.csv")
        val l3w = loader.loadMatrix("weights/layer_3_weights.csv")
        val l3b = loader.loadVector("weights/layer_3_biases.csv")
        NeuralNetwork(arrayOf(l1w, l2w, l3w), arrayOf(l1b, l2b, l3b))
    }

    val drawPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
        }
    }

    val drawingBitmap = remember {
        val b = createBitmap(50, 50)
        Canvas(b).drawColor(android.graphics.Color.BLACK)
        b
    }
    val drawingCanvas = remember { Canvas(drawingBitmap) }
    var prediction by remember { mutableStateOf<Int?>(null) }
    var drawCount by remember { mutableIntStateOf(0) }

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
                    "Оцените заведение",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Нарисуйте цифру от 0 до 9",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.Black)
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val start = change.position - dragAmount
                                val end = change.position
                                val scale = 50f / size.width.toFloat()
                                drawingCanvas.drawLine(
                                    start.x * scale, start.y * scale,
                                    end.x * scale, end.y * scale,
                                    drawPaint
                                )
                                drawCount++

                                val result = DigitRecognitionPrep.prepareBitmap(drawingBitmap, 1)
                                val input = bitmapToFloatArray(result)
                                val output = nn.forward(input)
                                prediction = output.indices.maxByOrNull { i -> output[i] }
                            }
                        }
                ) {
                    key(drawCount) {
                        Image(
                            bitmap = drawingBitmap.asImageBitmap(),
                            contentDescription = "Drawing Area",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Ваша оценка:", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = prediction?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            drawingCanvas.drawColor(android.graphics.Color.BLACK)
                            prediction = null
                            drawCount++
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сброс")
                    }
                    Button(
                        onClick = { prediction?.let { onRatingSubmitted(it) } },
                        enabled = prediction != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отправить")
                    }
                }
            }
        }
    }
}

private fun bitmapToFloatArray(bitmap: Bitmap): FloatArray {
    val width = bitmap.width
    val height = bitmap.height
    val floatArray = FloatArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = bitmap[x, y]
            val gray = (pixel and 0xff).toFloat() / 255.0f
            floatArray[y * width + x] = gray
        }
    }
    return floatArray
}
