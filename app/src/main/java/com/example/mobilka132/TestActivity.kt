package com.example.mobilka132

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import com.example.mobilka132.data.digit_recognition.DigitRecognitionPrep
import com.example.mobilka132.data.digit_recognition.ModelLoader
import com.example.mobilka132.data.digit_recognition.NeuralNetwork

class TestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val loader = ModelLoader(this)
        val layer1weights = loader.loadMatrix("weights/layer_1_weights.csv")
        val layer1bias = loader.loadVector("weights/layer_1_biases.csv")
        val layer2weights = loader.loadMatrix("weights/layer_2_weights.csv")
        val layer2bias = loader.loadVector("weights/layer_2_biases.csv")
        val layer3weights = loader.loadMatrix("weights/layer_3_weights.csv")
        val layer3bias = loader.loadVector("weights/layer_3_biases.csv")
        
        val nn = NeuralNetwork(
            arrayOf(
                layer1weights,
                layer2weights,
                layer3weights
            ),
            arrayOf(
                layer1bias,
                layer2bias,
                layer3bias
            )
        )
        
        enableEdgeToEdge()
        setContent {
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

            var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
            var prediction by remember { mutableStateOf<Int?>(null) }
            var drawCount by remember { mutableIntStateOf(0) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .systemBarsPadding()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Text("Draw Digit (Input 50x50)", style = MaterialTheme.typography.headlineSmall, color = Color.Black)

                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .border(2.dp, Color.Black)
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
                                processedBitmap = result
                                
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

                Text(
                    text = "Prediction: ${prediction ?: "-"}",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.Blue
                )

                Text("Pipeline Result (28x28)", style = MaterialTheme.typography.titleMedium, color = Color.Black)

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Color.DarkGray)
                        .border(1.dp, Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    processedBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Processed Result",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Button(
                    onClick = {
                        drawingCanvas.drawColor(android.graphics.Color.BLACK)
                        processedBitmap = null
                        prediction = null
                        drawCount++
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Clear Canvas")
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
}
