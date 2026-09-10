package com.nekogps.app.features.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * SimpleLineChart - a lightweight custom line chart drawn on Canvas.
 * Plots data points connected by lines, with filled area beneath.
 */
class SimpleLineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val LINE_WIDTH = 4f
        private const val FILL_ALPHA = 80
        private const val LABEL_TEXT_SIZE = 32f
        private const val PADDING_SIDE = 16f
        private const val PADDING_TOP = 24f
        private const val PADDING_BOTTOM = 48f
        private const val METERS_PER_KILOMETER = 1000
        private const val POINT_RADIUS = 6f
        private const val VALUE_LABEL_OFFSET_Y = 16
        private const val AXIS_LABEL_OFFSET_Y = 20
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#cbb7fb") // Lavender Glow
        strokeWidth = LINE_WIDTH
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#cbb7fb") // Lavender
        alpha = FILL_ALPHA
        style = Paint.Style.FILL
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#714cb6") // Amethyst
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#e9e5dd")
        textSize = LABEL_TEXT_SIZE
        textAlign = Paint.Align.CENTER
    }

    private var values: List<Float> = emptyList()
    private var labels: List<String> = emptyList()

    fun setData(values: List<Float>, labels: List<String>) {
        this.values = values
        this.labels = labels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.size < 2) return

        val width = width.toFloat()
        val height = height.toFloat()
        val paddingLeft = PADDING_SIDE
        val paddingRight = PADDING_SIDE
        val paddingTop = PADDING_TOP
        val paddingBottom = PADDING_BOTTOM
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val maxVal = values.maxOrNull() ?: 1f
        val minVal = values.minOrNull() ?: 0f
        val range = maxVal - minVal
        if (range == 0f) return

        val pointCount = values.size

        // Build path
        val path = Path()
        val points = mutableListOf<Float>()

        for (i in values.indices) {
            val x = paddingLeft + (i.toFloat() / (pointCount - 1)) * chartWidth
            val normalizedY = (values[i] - minVal) / range
            val y = paddingTop + chartHeight * (1f - normalizedY)
            points.add(x)
            points.add(y)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Draw filled area
        val fillPath = Path(path)
        fillPath.lineTo(points[points.size - 2], height - paddingBottom)
        fillPath.lineTo(points[0], height - paddingBottom)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)

        // Draw line
        canvas.drawPath(path, linePaint)

        // Draw points
        for (i in values.indices) {
            val x = paddingLeft + (i.toFloat() / (pointCount - 1)) * chartWidth
            val normalizedY = (values[i] - minVal) / range
            val y = paddingTop + chartHeight * (1f - normalizedY)
            canvas.drawCircle(x, y, POINT_RADIUS, pointPaint)

            // Value label
            textPaint.color = Color.parseColor("#cbb7fb")
            val valText = if (values[i] >= METERS_PER_KILOMETER) {
                "%.1fkm".format(values[i] / METERS_PER_KILOMETER)
            } else {
                "%.0fm".format(values[i])
            }
            canvas.drawText(valText, x, y - VALUE_LABEL_OFFSET_Y, textPaint)
        }

        // X-axis labels
        for (i in labels.indices) {
            val x = paddingLeft + (i.toFloat() / (pointCount - 1)) * chartWidth
            textPaint.color = Color.parseColor("#dcd7d3")
            canvas.drawText(labels[i], x, height - paddingBottom + AXIS_LABEL_OFFSET_Y, textPaint)
        }
    }
}
