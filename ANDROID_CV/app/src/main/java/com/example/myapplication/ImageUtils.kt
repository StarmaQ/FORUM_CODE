package com.example.myapplication

import android.graphics.*
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val ySize = yPlane.buffer.remaining()
        val uSize = uPlane.buffer.remaining()
        val vSize = vPlane.buffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yPlane.buffer.get(nv21, 0, ySize)

        // VU for NV21
        val uvPixelStride = uPlane.pixelStride
        val uvRowStride = uPlane.rowStride
        val width = image.width
        val height = image.height

        var pos = ySize
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val rowData = ByteArray(uvRowStride)
        var row = 0
        while (row < height / 2) {
            val bytesPerPixel = uvPixelStride
            val length = width / 2 * bytesPerPixel
            uBuffer.position(row * uvRowStride)
            vBuffer.position(row * uvRowStride)
            uBuffer.get(rowData, 0, uvRowStride)
            vBuffer.get(rowData, 0, uvRowStride) // reuse rowData variable

            var col = 0
            while (col < width / 2) {
                val vuIndex = col * uvPixelStride
                val v = vBuffer.get(row * uvRowStride + vuIndex)
                val u = uBuffer.get(row * uvRowStride + vuIndex)
                nv21[pos++] = v
                nv21[pos++] = u
                col++
            }
            row++
        }

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 80, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
