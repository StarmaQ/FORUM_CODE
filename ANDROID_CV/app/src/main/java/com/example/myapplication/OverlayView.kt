package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

data class Detection(val box: RectF, val label: String, val score: Float)

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boxes = mutableListOf<Detection>()
    private var srcW: Int = 0
    private var srcH: Int = 0

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    private val textBgPaint = Paint().apply {
        color = 0x80000000.toInt()
        style = Paint.Style.FILL
    }

    fun updateDetections(newDetections: List<Detection>, sourceWidth: Int, sourceHeight: Int) {
        boxes.clear()
        boxes.addAll(newDetections)
        srcW = sourceWidth
        srcH = sourceHeight
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (srcW == 0 || srcH == 0) return

        val scaleX = width.toFloat() / srcW
        val scaleY = height.toFloat() / srcH
        val scale = minOf(scaleX, scaleY)
        val dx = (width - srcW * scale) / 2f
        val dy = (height - srcH * scale) / 2f

        boxes.forEach { det ->
            val rect = RectF(
                det.box.left * scale + dx,
                det.box.top * scale + dy,
                det.box.right * scale + dx,
                det.box.bottom * scale + dy
            )
            canvas.drawRect(rect, boxPaint)

            val label = "${det.label} ${(det.score * 100).toInt()}%"
            val pad = 6f
            val textW = textPaint.measureText(label)
            val textH = textPaint.fontMetrics.run { bottom - top }
            val bgRect = RectF(rect.left, rect.top - textH - 2 * pad, rect.left + textW + 2 * pad, rect.top)
            canvas.drawRoundRect(bgRect, 8f, 8f, textBgPaint)
            canvas.drawText(label, bgRect.left + pad, bgRect.bottom - pad, textPaint)
        }
    }
}
