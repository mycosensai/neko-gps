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

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#cbb7fb") // Lavender Glow
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#cbb7fb") // Lavender
        alpha = 80
        style = Paint.Style.FILL
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#714cb6") // Amethyst
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#e9e5dd")
        textSize = 32f
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
        val paddingLeft = 16f
        val paddingRight = 16f
        val paddingTop = 24f
        val paddingBottom = 48f
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
            canvas.drawCircle(x, y, 6f, pointPaint)

            // Value label
            textPaint.color = Color.parseColor("#cbb7fb")
            val valText = if (values[i] >= 1000) "%.1fkm".format(values[i] / 1000) else "%.0fm".format(values[i])
            canvas.drawText(valText, x, y - 16, textPaint)
        }

        // X-axis labels
        for (i in labels.indices) {
            val x = paddingLeft + (i.toFloat() / (pointCount - 1)) * chartWidth
            textPaint.color = Color.parseColor("#dcd7d3")
            canvas.drawText(labels[i], x, height - paddingBottom + 20, textPaint)
        }
    }
}
