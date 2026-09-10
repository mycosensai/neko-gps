package com.nekogps.app.features

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.nekogps.app.R
import kotlin.math.roundToInt

/**
 * Snapshot of elevation data handed to [ElevationProfileRenderer].
 */
data class ElevationChartData(
    val points: List<ElevationProfileView.ElevationPoint>,
    val currentPosition: Int,
    val totalAscent: Double,
    val totalDescent: Double,
    val minElevation: Double,
    val maxElevation: Double
)

/** Pixel bounds of the chart area. */
data class ChartBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/**
 * Canvas renderer for [ElevationProfileView].
 * Owns all paints and chart-drawing routines so the view stays focused
 * on data management and statistics.
 */
class ElevationProfileRenderer(context: Context) {

    companion object {
        private const val LINE_STROKE_WIDTH = 4f
        private const val FILL_ALPHA = 40
        private const val GRID_ALPHA = 60
        private const val GRID_STROKE_WIDTH = 1f
        private const val TITLE_TEXT_SIZE = 28f
        private const val LABEL_TEXT_SIZE = 22f
        private const val MARKER_LINE_WIDTH = 3f
        private const val DASH_ON_LENGTH = 10f
        private const val DASH_OFF_LENGTH = 10f
        private const val PADDING_LEFT = 80f
        private const val PADDING_RIGHT = 30f
        private const val PADDING_TOP = 60f
        private const val PADDING_BOTTOM = 60f
        private const val STATS_HEIGHT = 80f
        private const val GRID_LINE_COUNT = 5
        private const val POSITION_DOT_RADIUS = 10f
        private const val STATS_CENTER_OFFSET_X = 60
        private const val STATS_RIGHT_OFFSET_X = 100
        private const val AXIS_LABEL_X = 5f
        private const val AXIS_LABEL_Y_OFFSET = 8
        private const val AXIS_VALUE_OFFSET_X = 60
        private const val AXIS_VALUE_OFFSET_Y = 40
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lavender_glow)
        strokeWidth = LINE_STROKE_WIDTH
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lavender_glow)
        alpha = FILL_ALPHA
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        alpha = GRID_ALPHA
        strokeWidth = GRID_STROKE_WIDTH
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = TITLE_TEXT_SIZE
        typeface = Typeface.DEFAULT_BOLD
    }

    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = LABEL_TEXT_SIZE
    }

    private val currentPosPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.warning)
        style = Paint.Style.FILL
    }

    private val currentPosLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.warning)
        strokeWidth = MARKER_LINE_WIDTH
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(DASH_ON_LENGTH, DASH_OFF_LENGTH), 0f)
    }

    fun drawChart(canvas: Canvas, data: ElevationChartData) {
        if (data.points.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val chartTop = PADDING_TOP + STATS_HEIGHT
        val chartBottom = canvas.height - PADDING_BOTTOM
        val chartLeft = PADDING_LEFT
        val chartRight = canvas.width - PADDING_RIGHT

        // Draw stats at top
        drawStats(canvas, data)

        // Draw grid
        drawGrid(canvas, data, ChartBounds(chartLeft, chartTop, chartRight, chartBottom))

        // Draw elevation profile
        drawElevationProfile(canvas, data, ChartBounds(chartLeft, chartTop, chartRight, chartBottom))

        // Draw current position marker
        if (data.currentPosition in data.points.indices) {
            drawCurrentPosition(canvas, data, ChartBounds(chartLeft, chartTop, chartRight, chartBottom))
        }

        // Draw axis labels
        drawAxisLabels(canvas, data, chartTop, chartRight, chartBottom)
    }

    private fun drawEmptyState(canvas: Canvas) {
        val text = "No elevation data available"
        val x = canvas.width / 2f - textPaint.measureText(text) / 2
        val y = canvas.height / 2f
        canvas.drawText(text, x, y, textPaint)
    }

    private fun drawStats(canvas: Canvas, data: ElevationChartData) {
        val ascentText = "↑ ${data.totalAscent.roundToInt()}m"
        val descentText = "↓ ${data.totalDescent.roundToInt()}m"
        val currentAlt = if (data.currentPosition in data.points.indices) {
            "${data.points[data.currentPosition].elevation.roundToInt()}m"
        } else {
            "--"
        }

        canvas.drawText("Current: $currentAlt", PADDING_LEFT, PADDING_TOP, textPaint)
        canvas.drawText(ascentText, canvas.width / 2f - STATS_CENTER_OFFSET_X, PADDING_TOP, smallTextPaint)
        canvas.drawText(
            descentText,
            canvas.width - PADDING_RIGHT - STATS_RIGHT_OFFSET_X,
            PADDING_TOP,
            smallTextPaint
        )
    }

    private fun drawGrid(
        canvas: Canvas,
        data: ElevationChartData,
        bounds: ChartBounds
    ) {
        val range = data.maxElevation - data.minElevation
        if (range <= 0) return

        for (i in 0..GRID_LINE_COUNT) {
            val y = bounds.top + (bounds.bottom - bounds.top) * i / GRID_LINE_COUNT
            canvas.drawLine(bounds.left, y, bounds.right, y, gridPaint)
        }
    }

    private fun drawElevationProfile(
        canvas: Canvas,
        data: ElevationChartData,
        bounds: ChartBounds
    ) {
        if (data.points.size < 2) return

        val maxDist = data.points.maxOf { it.distance }
        val minDist = data.points.minOf { it.distance }
        val distRange = if (maxDist - minDist > 0) maxDist - minDist else 1.0
        val elevRange = elevationRange(data)

        val path = Path()
        val fillPath = Path()

        for (i in data.points.indices) {
            val point = data.points[i]
            val x = bounds.left + ((point.distance - minDist) / distRange * (bounds.right - bounds.left)).toFloat()
            val y = bounds.bottom - ((point.elevation - data.minElevation) / elevRange * (bounds.bottom - bounds.top))
                .toFloat()

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, bounds.bottom)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Close fill path
        fillPath.lineTo(bounds.right, bounds.bottom)
        fillPath.close()

        // Draw fill
        canvas.drawPath(fillPath, fillPaint)

        // Draw line
        canvas.drawPath(path, linePaint)
    }

    private fun drawCurrentPosition(
        canvas: Canvas,
        data: ElevationChartData,
        bounds: ChartBounds
    ) {
        val point = data.points[data.currentPosition]
        val maxDist = data.points.maxOf { it.distance }
        val minDist = data.points.minOf { it.distance }
        val distRange = if (maxDist - minDist > 0) maxDist - minDist else 1.0
        val elevRange = elevationRange(data)

        val x = bounds.left + ((point.distance - minDist) / distRange * (bounds.right - bounds.left)).toFloat()
        val y = bounds.bottom -
            ((point.elevation - data.minElevation) / elevRange * (bounds.bottom - bounds.top)).toFloat()

        // Draw vertical line
        canvas.drawLine(x, bounds.top, x, bounds.bottom, currentPosLinePaint)

        // Draw circle
        canvas.drawCircle(x, y, POSITION_DOT_RADIUS, currentPosPaint)
    }

    private fun drawAxisLabels(
        canvas: Canvas,
        data: ElevationChartData,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        // Y-axis labels (elevation)
        val elevRange = elevationRange(data)
        for (i in 0..GRID_LINE_COUNT) {
            val elev = data.minElevation + elevRange * (GRID_LINE_COUNT - i) / GRID_LINE_COUNT
            val y = top + (bottom - top) * i / GRID_LINE_COUNT
            canvas.drawText("${elev.roundToInt()}m", AXIS_LABEL_X, y + AXIS_LABEL_Y_OFFSET, smallTextPaint)
        }

        // X-axis label
        val maxDist = data.points.maxOf { it.distance }
        canvas.drawText(
            "${maxDist.roundToInt()} km",
            right - AXIS_VALUE_OFFSET_X,
            bottom + AXIS_VALUE_OFFSET_Y,
            smallTextPaint
        )
    }

    private fun elevationRange(data: ElevationChartData): Double {
        return if (data.maxElevation - data.minElevation > 0) {
            data.maxElevation - data.minElevation
        } else {
            1.0
        }
    }
}
