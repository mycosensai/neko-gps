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

    companion object {
        private const val VALUE_TEXT_SIZE = 36f
        private const val LABEL_TEXT_SIZE = 28f
        private const val PADDING_SIDE = 16f
        private const val PADDING_TOP = 24f
        private const val PADDING_BOTTOM = 48f
        private const val BAR_WIDTH_RATIO = 0.7f
        private const val GAP_RATIO = 0.3f
        private const val MAX_BAR_HEIGHT_RATIO = 0.8f
        private const val METERS_PER_KILOMETER = 1000
        private const val VALUE_LABEL_OFFSET_Y = 8
        private const val AXIS_LABEL_OFFSET_Y = 20
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#cbb7fb") // Lavender Glow
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#e9e5dd") // Warm Cream
        textSize = VALUE_TEXT_SIZE
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#dcd7d3") // Parchment
        textSize = LABEL_TEXT_SIZE
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
        val paddingLeft = PADDING_SIDE
        val paddingRight = PADDING_SIDE
        val paddingTop = PADDING_TOP
        val paddingBottom = PADDING_BOTTOM
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        val barCount = values.size
        val barWidth = (chartWidth / barCount) * BAR_WIDTH_RATIO
        val gap = (chartWidth / barCount) * GAP_RATIO

        val maxBarHeight = chartHeight * MAX_BAR_HEIGHT_RATIO

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
            val valueText = if (value >= METERS_PER_KILOMETER) {
                "%.1fkm".format(value / METERS_PER_KILOMETER)
            } else {
                "%.0fm".format(value)
            }
            canvas.drawText(valueText, x + barWidth / 2, y - VALUE_LABEL_OFFSET_Y, textPaint)

            // X-axis label
            labelPaint.color = Color.parseColor("#dcd7d3")
            if (i < labels.size) {
                canvas.drawText(labels[i], x + barWidth / 2, height - paddingBottom + AXIS_LABEL_OFFSET_Y, labelPaint)
            }
        }
    }
}
