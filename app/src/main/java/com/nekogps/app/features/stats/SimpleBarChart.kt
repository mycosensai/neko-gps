package com.nekogps.app.features.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * SimpleBarChart - a lightweight custom bar chart drawn on Canvas.
 * Used because MPAndroidChart may have import resolution issues.
 */
class SimpleBarChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#cbb7fb") // Lavender Glow
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#714cb6") // Amethyst
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#e9e5dd") // Warm Cream
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#dcd7d3") // Parchment
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private var values: List<Float> = emptyList()
    private var labels: List<String> = emptyList()
    private var maxValue: Float = 1f

    fun setData(values: List<Float>, labels: List<String>) {
        this.values = values
        this.labels = labels
        this.maxValue = values.maxOrNull() ?: 1f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val paddingLeft = 16f
        val paddingRight = 16f
        val paddingTop = 24f
        val paddingBottom = 48f
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        val barCount = values.size
        val barWidth = (chartWidth / barCount) * 0.7f
        val gap = (chartWidth / barCount) * 0.3f

        val maxBarHeight = chartHeight * 0.8f

        for (i in values.indices) {
            val value = values[i]
            val barHeight = (value / maxValue) * maxBarHeight
            val x = paddingLeft + i * (barWidth + gap) + gap / 2
            val y = height - paddingBottom - barHeight

            // Draw bar
            barPaint.color = if (i % 2 == 0) {
                Color.parseColor("#cbb7fb") // Lavender
            } else {
                Color.parseColor("#714cb6") // Amethyst
            }
            canvas.drawRect(x, y, x + barWidth, height - paddingBottom, barPaint)

            // Value label on top
            textPaint.color = Color.parseColor("#cbb7fb")
            val valueText = if (value >= 1000) "%.1fkm".format(value / 1000) else "%.0fm".format(value)
            canvas.drawText(valueText, x + barWidth / 2, y - 8, textPaint)

            // X-axis label
            labelPaint.color = Color.parseColor("#dcd7d3")
            if (i < labels.size) {
                canvas.drawText(labels[i], x + barWidth / 2, height - paddingBottom + 20, labelPaint)
            }
        }
    }
}
