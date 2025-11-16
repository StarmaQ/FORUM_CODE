package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CarDetector(context: Context) {
    private var interpreter: Interpreter? = null
    private val inputSize = 640 // must match export size
    private val scoreThresh = 0.25f
    private val carClassId = 2f // COCO: car = 2

    init {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, "models/car_detector.tflite")
            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            Log.w("CarDetector", "TFLite model not found or failed to load: ${e.message}")
        }
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        val tflite = interpreter ?: return emptyList()

        val srcW = bitmap.width
        val srcH = bitmap.height
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = bitmapToFloatBuffer(resized)

        val outputsCount = try { tflite.outputTensorCount } catch (_: Throwable) { 1 }
        val results = mutableListOf<Detection>()

        return try {
            if (outputsCount == 1) {
                // Expect [1, N, 6] => x1,y1,x2,y2,score,class
                val outShape = tflite.getOutputTensor(0).shape()
                val outBuf = TensorBuffer.createFixedSize(outShape, DataType.FLOAT32)
                val outputs = hashMapOf<Int, Any>(0 to outBuf.buffer)
                tflite.runForMultipleInputsOutputs(arrayOf(input), outputs)

                val floats = outBuf.floatArray
                val n = if (outShape.size >= 3) outShape[1] else (floats.size / 6)
                val sx = srcW.toFloat() / inputSize
                val sy = srcH.toFloat() / inputSize

                var idx = 0
                for (i in 0 until n) {
                    val x1 = floats[idx++]
                    val y1 = floats[idx++]
                    val x2 = floats[idx++]
                    val y2 = floats[idx++]
                    val score = floats[idx++]
                    val cls = floats[idx++]
                    if (score >= scoreThresh && cls == carClassId) {
                        val rect = RectF(x1 * sx, y1 * sy, x2 * sx, y2 * sy)
                        results.add(Detection(rect, "car", score))
                    }
                }
                results
            } else {
                // Fallback: separate outputs: boxes[1,N,4], scores[1,N], classes[1,N] (optional: count)
                val boxesT = tflite.getOutputTensor(0)
                val scoresT = tflite.getOutputTensor(1)
                val classesT = tflite.getOutputTensor(2)

                val boxesB = TensorBuffer.createFixedSize(boxesT.shape(), DataType.FLOAT32)
                val scoresB = TensorBuffer.createFixedSize(scoresT.shape(), DataType.FLOAT32)
                val classesB = TensorBuffer.createFixedSize(classesT.shape(), DataType.FLOAT32)

                val outputs = hashMapOf<Int, Any>(
                    0 to boxesB.buffer,
                    1 to scoresB.buffer,
                    2 to classesB.buffer
                )
                tflite.runForMultipleInputsOutputs(arrayOf(input), outputs)

                val boxes = boxesB.floatArray
                val scores = scoresB.floatArray
                val classes = classesB.floatArray
                val n = minOf(scores.size, classes.size, boxes.size / 4)
                val sx = srcW.toFloat() / inputSize
                val sy = srcH.toFloat() / inputSize

                var bi = 0
                for (i in 0 until n) {
                    val x1 = boxes[bi++]
                    val y1 = boxes[bi++]
                    val x2 = boxes[bi++]
                    val y2 = boxes[bi++]
                    val score = scores[i]
                    val cls = classes[i]
                    if (score >= scoreThresh && cls == carClassId) {
                        val rect = RectF(x1 * sx, y1 * sy, x2 * sx, y2 * sy)
                        results.add(Detection(rect, "car", score))
                    }
                }
                results
            }
        } catch (e: Exception) {
            Log.w("CarDetector", "Inference/parse error: ${e.message}")
            emptyList()
        }
    }

    private fun bitmapToFloatBuffer(bitmap: Bitmap): ByteBuffer {
        val imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        imgData.order(ByteOrder.nativeOrder())
        imgData.rewind()
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)
        var i = 0
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val v = intValues[i++]
                val r = (v shr 16 and 0xFF) / 255f
                val g = (v shr 8 and 0xFF) / 255f
                val b = (v and 0xFF) / 255f
                imgData.putFloat(r)
                imgData.putFloat(g)
                imgData.putFloat(b)
            }
        }
        imgData.rewind()
        return imgData
    }
}
