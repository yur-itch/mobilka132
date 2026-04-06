package com.example.mobilka132.data.digit_recognition

import android.util.Log
import kotlin.math.exp

class NeuralNetwork(
    private val weights: Array<Array<FloatArray>>,
    private val bias: Array<FloatArray>
) {

    fun forward(input: FloatArray): FloatArray {
        var x = input

        Log.d("NN_DEBUG", "Input size: ${input.size}")
        Log.d("NN_DEBUG", "Weights rows: ${weights.size}")
        Log.d("NN_DEBUG", "Weights cols: ${weights[0].size}")
        Log.d("NN_DEBUG", "Biases size: ${bias.size}")

        for (layerIndex in weights.indices) {
            val layerWeights = weights[layerIndex]
            val layerBias = bias[layerIndex]
            val newX = FloatArray(layerWeights.size)

            for (i in layerWeights.indices) {
                var sum = 0.0f
                for (j in layerWeights[i].indices) {
                    sum += layerWeights[i][j] * x[j]
                }
                sum += layerBias[i]
                newX[i] = if (layerIndex != weights.lastIndex) activation(sum) else sum
            }

            x = newX
        }

        return softmax(x)
    }

    private fun activation(x: Float): Float {
        return if (x > 0) x else 0.0f
    }

    private fun softmax(x: FloatArray): FloatArray {
        val max = x.maxOrNull() ?: 0.0f
        val expValues = FloatArray(x.size)
        var sum = 0.0f
        for (i in x.indices) {
            expValues[i] = exp(x[i] - max)
            sum += expValues[i]
        }
        for (i in expValues.indices) {
            expValues[i] /= sum
        }
        return expValues
    }
}