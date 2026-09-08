package com.nekogps.app.features

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.nekogps.app.R
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

/**
 * Custom View that draws an elevation chart using MPAndroidChart.
 * Shows altitude profile for current track or route.
 * Displays current altitude, total ascent, total descent.
 */
class ElevationProfileView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Data
    private var elevationPoints: List<ElevationPoint> = emptyList()
    private var currentPosition: Int = -1

    // Statistics
    private var totalAscent: Double = 0.0
    private var totalDescent: Double = 0.0
    private var minElevation: Double = Double.MAX_VALUE
    private var maxElevation: Double = Double.MIN_VALUE

    // Drawing
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lavender_glow)
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.lavender_glow)
        alpha = 40
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        alpha = 60
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
    }

    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 22f
    }

    private val currentPosPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.warning)
        style = Paint.Style.FILL
    }

    private val currentPosLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.warning)
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    // Layout
    private val paddingLeft = 80f
    private val paddingRight = 30f
    private val paddingTop = 60f
    private val paddingBottom = 60f
    private val statsHeight = 80f

    data class ElevationPoint(
        val distance: Double, // km from start
        val elevation: Double, // meters
        val geoPoint: GeoPoint? = null
    )

    fun setElevationData(points: List<ElevationPoint>) {
        elevationPoints = points
        calculateStatistics()
        invalidate()
    }

    fun setCurrentPosition(index: Int) {
        currentPosition = index
        invalidate()
    }

    fun setTrackFromGeoPoints(geoPoints: List<GeoPoint>) {
        val points = mutableListOf<ElevationPoint>()
        var totalDist = 0.0

        for (i in geoPoints.indices) {
            val gp = geoPoints[i]
            if (i > 0) {
                totalDist += calculateDistance(geoPoints[i - 1], gp)
            }
            points.add(ElevationPoint(totalDist, gp.altitude, gp))
        }

        setElevationData(points)
    }

    private fun calculateDistance(p1: GeoPoint, p2: GeoPoint): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            p1.latitude, p1.longitude,
            p2.latitude, p2.longitude,
            results
        )
        return results[0] / 1000.0 // Convert to km
    }

    private fun calculateStatistics() {
        if (elevationPoints.isEmpty()) {
            totalAscent = 0.0
            totalDescent = 0.0
            minElevation = 0.0
            maxElevation = 0.0
            return
        }

        totalAscent = 0.0
        totalDescent = 0.0
        minElevation = elevationPoints.minOf { it.elevation }
        maxElevation = elevationPoints.maxOf { it.elevation }

        for (i in 1 until elevationPoints.size) {
            val diff = elevationPoints[i].elevation - elevationPoints[i - 1].elevation
            if (diff > 0) totalAscent += diff
            else totalDescent += -diff
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (elevationPoints.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val chartTop = paddingTop + statsHeight
        val chartBottom = height - paddingBottom
        val chartLeft = paddingLeft
        val chartRight = width - paddingRight

        // Draw stats at top
        drawStats(canvas)

        // Draw grid
        drawGrid(canvas, chartLeft, chartTop, chartRight, chartBottom)

        // Draw elevation profile
        drawElevationProfile(canvas, chartLeft, chartTop, chartRight, chartBottom)

        // Draw current position marker
        if (currentPosition in elevationPoints.indices) {
            drawCurrentPosition(canvas, chartLeft, chartTop, chartRight, chartBottom)
        }

        // Draw axis labels
        drawAxisLabels(canvas, chartLeft, chartTop, chartRight, chartBottom)
    }

    private fun drawEmptyState(canvas: Canvas) {
        val text = "No elevation data available"
        val x = width / 2f - textPaint.measureText(text) / 2
        val y = height / 2f
        canvas.drawText(text, x, y, textPaint)
    }

    private fun drawStats(canvas: Canvas) {
        val ascentText = "↑ ${totalAscent.roundToInt()}m"
        val descentText = "↓ ${totalDescent.roundToInt()}m"
        val currentAlt = if (currentPosition in elevationPoints.indices)
            "${elevationPoints[currentPosition].elevation.roundToInt()}m" else "--"

        canvas.drawText("Current: $currentAlt", paddingLeft, paddingTop, textPaint)
        canvas.drawText(ascentText, width / 2f - 60, paddingTop, smallTextPaint)
        canvas.drawText(descentText, width - paddingRight - 100, paddingTop, smallTextPaint)
    }

    private fun drawGrid(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val range = maxElevation - minElevation
        if (range <= 0) return

        val gridLines = 5
        for (i in 0..gridLines) {
            val y = top + (bottom - top) * i / gridLines
            canvas.drawLine(left, y, right, y, gridPaint)
        }
    }

    private fun drawElevationProfile(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        if (elevationPoints.size < 2) return

        val maxDist = elevationPoints.maxOf { it.distance }
        val minDist = elevationPoints.minOf { it.distance }
        val distRange = if (maxDist - minDist > 0) maxDist - minDist else 1.0
        val elevRange = if (maxElevation - minElevation > 0) maxElevation - minElevation else 1.0

        val path = Path()
        val fillPath = Path()

        for (i in elevationPoints.indices) {
            val point = elevationPoints[i]
            val x = left + ((point.distance - minDist) / distRange * (right - left)).toFloat()
            val y = bottom - ((point.elevation - minElevation) / elevRange * (bottom - top)).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, bottom)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Close fill path
        fillPath.lineTo(right, bottom)
        fillPath.close()

        // Draw fill
        canvas.drawPath(fillPath, fillPaint)

        // Draw line
        canvas.drawPath(path, linePaint)
    }

    private fun drawCurrentPosition(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        val point = elevationPoints[currentPosition]
        val maxDist = elevationPoints.maxOf { it.distance }
        val minDist = elevationPoints.minOf { it.distance }
        val distRange = if (maxDist - minDist > 0) maxDist - minDist else 1.0
        val elevRange = if (maxElevation - minElevation > 0) maxElevation - minElevation else 1.0

        val x = left + ((point.distance - minDist) / distRange * (right - left)).toFloat()
        val y = bottom - ((point.elevation - minElevation) / elevRange * (bottom - top)).toFloat()

        // Draw vertical line
        canvas.drawLine(x, top, x, bottom, currentPosLinePaint)

        // Draw circle
        canvas.drawCircle(x, y, 10f, currentPosPaint)
    }

    private fun drawAxisLabels(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        // Y-axis labels (elevation)
        val elevRange = if (maxElevation - minElevation > 0) maxElevation - minElevation else 1.0
        val gridLines = 5
        for (i in 0..gridLines) {
            val elev = minElevation + elevRange * (gridLines - i) / gridLines
            val y = top + (bottom - top) * i / gridLines
            canvas.drawText("${elev.roundToInt()}m", 5f, y + 8, smallTextPaint)
        }

        // X-axis label
        val maxDist = elevationPoints.maxOf { it.distance }
        canvas.drawText(
            "${maxDist.roundToInt()} km",
            right - 60,
            bottom + 40,
            smallTextPaint
        )
    }

    fun getCurrentAltitude(): Double? {
        return if (currentPosition in elevationPoints.indices)
            elevationPoints[currentPosition].elevation else null
    }

    fun getTotalAscent(): Double = totalAscent
    fun getTotalDescent(): Double = totalDescent
}
