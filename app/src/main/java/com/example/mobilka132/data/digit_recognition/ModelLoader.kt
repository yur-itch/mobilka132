package com.example.mobilka132.data.digit_recognition

import java.io.BufferedReader
import java.io.InputStreamReader

class ModelLoader(private val context: android.content.Context) {
    fun loadMatrix(fileName: String): Array<FloatArray> {
        val matrix = mutableListOf<FloatArray>()
        val reader = BufferedReader(InputStreamReader(context.assets.open(fileName)))

        reader.useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank()) {
                    val row = line.split(",").map { it.trim().toFloat() }.toFloatArray()
                    matrix.add(row)
                }
            }
        }
        return matrix.toTypedArray()
    }

    fun loadVector(fileName: String): FloatArray {
        val assetManager = context.assets
        val text = assetManager.open(fileName).bufferedReader().use { it.readText() }

        return text.split(Regex("[,\\s\\n\\r]+")) // Splits by comma, space, or newline
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.toFloat() }
            .toFloatArray()
    }
}