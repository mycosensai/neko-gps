package com.nekogps.app.features

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
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

    companion object {
        private const val METERS_PER_KILOMETER = 1000.0
    }

    // Data
    private var elevationPoints: List<ElevationPoint> = emptyList()
    private var currentPosition: Int = -1

    // Statistics
    private var totalAscent: Double = 0.0
    private var totalDescent: Double = 0.0
    private var minElevation: Double = Double.MAX_VALUE
    private var maxElevation: Double = Double.MIN_VALUE

    private val renderer = ElevationProfileRenderer(context)

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
        return results[0] / METERS_PER_KILOMETER // Convert to km
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
        val data = ElevationChartData(
            points = elevationPoints,
            currentPosition = currentPosition,
            totalAscent = totalAscent,
            totalDescent = totalDescent,
            minElevation = minElevation,
            maxElevation = maxElevation
        )
        renderer.drawChart(canvas, data)
    }

    fun getCurrentAltitude(): Double? {
        return if (currentPosition in elevationPoints.indices)
            elevationPoints[currentPosition].elevation else null
    }

    fun getTotalAscent(): Double = totalAscent
    fun getTotalDescent(): Double = totalDescent
}
