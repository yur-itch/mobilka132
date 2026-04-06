package com.example.mobilka132.data.digit_recognition

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.geometry.Offset
import androidx.core.graphics.createBitmap
import kotlin.math.pow
import androidx.core.graphics.set
import androidx.core.graphics.get
import kotlin.math.exp
import androidx.core.graphics.scale

class DigitRecognitionPrep {
    companion object {
        private fun dilate(img: Bitmap, radius: Int): Bitmap {
            val kernel = createBitmap(radius * 2 + 1, radius * 2 + 1)
            val centerX = kernel.width / 2
            val centerY = kernel.height / 2

            for (i in 0 until kernel.width) {
                for (j in 0 until kernel.height) {
                    val x2 = (i - centerX).toDouble().pow(2.0)
                    val y2 = (j - centerY).toDouble().pow(2.0)
                    val dist = (x2 + y2).pow(0.5)
                    if (dist <= radius) {
                        kernel[i, j] = Color.WHITE
                    }
                }
            }

            val result = createBitmap(img.width, img.height, Bitmap.Config.ARGB_8888)
            Canvas(result).drawColor(android.graphics.Color.BLACK)
            val canvas = Canvas(result)

            for (i in 0 until img.width) {
                for (j in 0 until img.height) {
                    if ((img[i, j] and 0xff) > 0) {
                        canvas.drawBitmap(
                            kernel,
                            (i - radius).toFloat(),
                            (j - radius).toFloat(),
                            null
                        )
                    }
                }
            }

            return result
        }

        private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
            val matrix = Matrix().apply {
                postRotate(angle, source.width / 2f, source.height / 2f)
            }
            val res = Bitmap.createBitmap(
                source,
                0,
                0,
                source.width,
                source.height,
                matrix,
                true
            )
            Canvas(res).drawColor(android.graphics.Color.BLACK)
            return res
        }

        private fun otsuBinarize(src: Bitmap): Bitmap {
            val width = src.width
            val height = src.height
            val histogram = IntArray(256)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = src[x, y]
                    val r = (pixel shr 16) and 0xff
                    val g = (pixel shr 8) and 0xff
                    val b = pixel and 0xff
                    val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
                    histogram[gray]++
                }
            }

            val total = width * height
            var sum = 0.0
            for (i in 0..255) sum += i * histogram[i]

            var sumB = 0.0
            var wB = 0
            var wF: Int
            var maxVariance = 0.0
            var threshold = 0

            for (t in 0..255) {
                wB += histogram[t]
                if (wB == 0) continue
                wF = total - wB
                if (wF == 0) break
                sumB += t.toDouble() * histogram[t]
                val mB = sumB / wB
                val mF = (sum - sumB) / wF
                val varianceBetween = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)

                if (varianceBetween > maxVariance) {
                    maxVariance = varianceBetween
                    threshold = t
                }
            }

            val result = createBitmap(width, height)
            Canvas(result).drawColor(android.graphics.Color.BLACK)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = src[x, y]
                    val r = (pixel shr 16) and 0xff
                    val g = (pixel shr 8) and 0xff
                    val b = pixel and 0xff
                    val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                    val binary = if (gray >= threshold) 255 else 0

                    val newPixel = (0xff shl 24) or (binary shl 16) or (binary shl 8) or binary
                    result[x, y] = newPixel
                }
            }

            return result
        }

        private fun skeletonize(src: Bitmap): Bitmap {
            val width = src.width
            val height = src.height
            val img = Array(height) { IntArray(width) }

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = src[x, y] and 0xff
                    img[y][x] = if (pixel > 0) 1 else 0
                }
            }

            var changed: Boolean

            do {
                changed = false
                val toRemove = mutableListOf<Pair<Int, Int>>()
                for (y in 1 until height - 1) {
                    for (x in 1 until width - 1) {
                        if (img[y][x] != 1) continue
                        val p2 = img[y - 1][x]
                        val p3 = img[y - 1][x + 1]
                        val p4 = img[y][x + 1]
                        val p5 = img[y + 1][x + 1]
                        val p6 = img[y + 1][x]
                        val p7 = img[y + 1][x - 1]
                        val p8 = img[y][x - 1]
                        val p9 = img[y - 1][x - 1]
                        val neighbors = p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9
                        if (neighbors !in 2..6) continue
                        val transitions = countTransitions(p2, p3, p4, p5, p6, p7, p8, p9)
                        if (transitions != 1) continue
                        if (p2 * p4 * p6 != 0) continue
                        if (p4 * p6 * p8 != 0) continue
                        toRemove.add(x to y)
                    }
                }

                if (toRemove.isNotEmpty()) changed = true
                for ((x, y) in toRemove) img[y][x] = 0
                toRemove.clear()

                for (y in 1 until height - 1) {
                    for (x in 1 until width - 1) {
                        if (img[y][x] != 1) continue
                        val p2 = img[y - 1][x]
                        val p3 = img[y - 1][x + 1]
                        val p4 = img[y][x + 1]
                        val p5 = img[y + 1][x + 1]
                        val p6 = img[y + 1][x]
                        val p7 = img[y + 1][x - 1]
                        val p8 = img[y][x - 1]
                        val p9 = img[y - 1][x - 1]
                        val neighbors = p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9
                        if (neighbors !in 2..6) continue
                        val transitions = countTransitions(p2, p3, p4, p5, p6, p7, p8, p9)
                        if (transitions != 1) continue
                        if (p2 * p4 * p8 != 0) continue
                        if (p2 * p6 * p8 != 0) continue
                        toRemove.add(x to y)
                    }
                }

                if (toRemove.isNotEmpty()) changed = true
                for ((x, y) in toRemove) img[y][x] = 0

            } while (changed)

            val result = createBitmap(width, height)
            Canvas(result).drawColor(android.graphics.Color.BLACK)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val v = if (img[y][x] == 1) 255 else 0
                    val pixel = (0xff shl 24) or (v shl 16) or (v shl 8) or v
                    result[x, y] = pixel
                }
            }

            return result
        }

        private fun countTransitions(
            p2: Int, p3: Int, p4: Int, p5: Int,
            p6: Int, p7: Int, p8: Int, p9: Int
        ): Int {
            val arr = intArrayOf(p2, p3, p4, p5, p6, p7, p8, p9, p2)
            var transitions = 0

            for (i in 0 until 8) {
                if (arr[i] == 0 && arr[i + 1] == 1) {
                    transitions++
                }
            }

            return transitions
        }

        private fun gaussianBlur(src: Bitmap, radius: Int, sigma: Float): Bitmap {
            val width = src.width
            val height = src.height
            val result = createBitmap(width, height)
            Canvas(result).drawColor(android.graphics.Color.BLACK)
            val kernelSize = 2 * radius + 1
            val kernel = FloatArray(kernelSize)
            val sigma2 = 2 * sigma * sigma
            var sum = 0f

            for (i in -radius..radius) {
                val value = exp((-(i * i) / sigma2).toDouble()).toFloat()
                kernel[i + radius] = value
                sum += value
            }

            for (i in kernel.indices) kernel[i] /= sum

            val temp = Array(height) { IntArray(width) }

            for (y in 0 until height) {
                for (x in 0 until width) {
                    var rSum = 0f
                    var gSum = 0f
                    var bSum = 0f

                    for (k in -radius..radius) {
                        val xn = (x + k).coerceIn(0, width - 1)
                        val pixel = src[xn, y]
                        val weight = kernel[k + radius]

                        rSum += ((pixel shr 16) and 0xff) * weight
                        gSum += ((pixel shr 8) and 0xff) * weight
                        bSum += (pixel and 0xff) * weight
                    }

                    val r = rSum.toInt().coerceIn(0, 255)
                    val g = gSum.toInt().coerceIn(0, 255)
                    val b = bSum.toInt().coerceIn(0, 255)

                    temp[y][x] = (r shl 16) or (g shl 8) or b
                }
            }

            for (y in 0 until height) {
                for (x in 0 until width) {
                    var rSum = 0f
                    var gSum = 0f
                    var bSum = 0f

                    for (k in -radius..radius) {
                        val yn = (y + k).coerceIn(0, height - 1)
                        val pixel = temp[yn][x]
                        val weight = kernel[k + radius]

                        rSum += ((pixel shr 16) and 0xff) * weight
                        gSum += ((pixel shr 8) and 0xff) * weight
                        bSum += (pixel and 0xff) * weight
                    }

                    val r = rSum.toInt().coerceIn(0, 255)
                    val g = gSum.toInt().coerceIn(0, 255)
                    val b = bSum.toInt().coerceIn(0, 255)
                    result[x, y] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
                }
            }

            return result
        }

        private fun cropBitmap(original: Bitmap, boundingRect: Rect): Bitmap {
            val left = boundingRect.left.coerceIn(0, original.width)
            val top = boundingRect.top.coerceIn(0, original.height)
            val width = boundingRect.width().coerceAtMost(original.width - left)
            val height = boundingRect.height().coerceAtMost(original.height - top)

            val res = Bitmap.createBitmap(original, left, top, width, height)
            return res
        }

        private fun cropBoundingBox(bitmap: Bitmap): Bitmap {
            var xMin = bitmap.width
            var xMax = 0
            var yMin = bitmap.height
            var yMax = 0
            var hasPixel = false

            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    if ((bitmap[x, y] and 0xff) > 0) {
                        hasPixel = true
                        xMin = minOf(xMin, x)
                        xMax = maxOf(xMax, x)
                        yMin = minOf(yMin, y)
                        yMax = maxOf(yMax, y)
                    }
                }
            }

            if (!hasPixel) {
                return bitmap
            }

            val boundingBox = Rect(xMin, yMin, xMax + 1, yMax + 1)
            return cropBitmap(bitmap, boundingBox)
        }

        private fun centerOfMass(bitmap: Bitmap): Offset {
            var sumX = 0f
            var sumY = 0f
            var count = 0f

            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    val v = bitmap[x, y] and 0xff
                    if (v > 0) {
                        sumX += x
                        sumY += y
                        count += 1f
                    }
                }
            }

            return if (count > 0f) {
                Offset(sumX / count, sumY / count)
            } else {
                Offset(bitmap.width / 2f, bitmap.height / 2f)
            }
        }

        private fun resizeMaintainingAspectRatio(source: Bitmap, targetMaxDim: Float): Bitmap {
            val w = source.width
            val h = source.height
            val maxDim = maxOf(w, h).toFloat()

            val scale = targetMaxDim / maxDim
            val nw = maxOf(1, (w * scale).toInt())
            val nh = maxOf(1, (h * scale).toInt())

            return source.scale(nw, nh)
        }

        private fun scaleAndCenterTo28x28(cropped: Bitmap): Bitmap {
            val w = cropped.width
            val h = cropped.height
            val maxDim = maxOf(w, h).toFloat()
            val scale = 20f / maxDim
            val nw = maxOf(1, (w * scale).toInt())
            val nh = maxOf(1, (h * scale).toInt())
            val resized = cropped.scale(nw, nh)
            val center = centerOfMass(resized)
            val shiftX = 14f - center.x
            val shiftY = 14f - center.y
            val canvasBitmap = createBitmap(28, 28)
            Canvas(canvasBitmap).drawColor(android.graphics.Color.BLACK)
            val canvas = Canvas(canvasBitmap)
            val matrix = Matrix().apply {
                postTranslate(shiftX, shiftY)
            }
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(resized, matrix, paint)
            return canvasBitmap
        }

        fun padBitmap(
            source: Bitmap,
            paddingLeft: Int,
            paddingTop: Int,
            paddingRight: Int,
            paddingBottom: Int
        ): Bitmap {
            val newWidth = source.width + paddingLeft + paddingRight
            val newHeight = source.height + paddingTop + paddingBottom
            val paddedBitmap = createBitmap(newWidth, newHeight, source.config!!)
            val canvas = Canvas(paddedBitmap)
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(source, paddingLeft.toFloat(), paddingTop.toFloat(), null)
            return paddedBitmap
        }

        fun prepareBitmap(img: Bitmap, radius: Int): Bitmap {
            var res = cropBoundingBox(img)
            res = resizeMaintainingAspectRatio(res, 50.0f)
            res = otsuBinarize(res)
            res = padBitmap(res, 1, 1, 1, 1)
            res = skeletonize(res)
            res = dilate(res, 3)
            res = scaleAndCenterTo28x28(res)
            res = gaussianBlur(res, 1, 0.5f)

            return res
        }
    }
}