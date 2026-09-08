package com.nekogps.app.features

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.nekogps.app.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.roundToInt

/**
 * Traffic overlay that shows congestion levels as colored polylines on the map.
 * Uses estimated traffic data from OpenStreetMap or TomTom Traffic API.
 * Includes a legend overlay.
 */
class TrafficLayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var showLegend = true
    private val legendItems = listOf(
        LegendItem("No Traffic", Color.parseColor("#4CAF50")),
        LegendItem("Light", Color.parseColor("#8BC34A")),
        LegendItem("Moderate", Color.parseColor("#FF9800")),
        LegendItem("Heavy", Color.parseColor("#F44336")),
        LegendItem("Standstill", Color.parseColor("#8B0000"))
    )

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC292827")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E9E5DD")
        textSize = 24f
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CBB7FB")
        textSize = 28f
        isFakeBoldText = true
    }

    data class LegendItem(val label: String, val color: Int)

    fun toggleLegend() {
        showLegend = !showLegend
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!showLegend) return

        val itemHeight = 40f
        val padding = 20f
        val boxSize = 24f
        val textOffset = 40f

        val width = 220f
        val height = padding * 2 + titlePaint.textSize + itemHeight * legendItems.size

        // Position in bottom-left corner
        val left = 30f
        val top = height - 30f - height

        // Draw background
        canvas.drawRoundRect(left, top, left + width, top + height, 16f, 16f, backgroundPaint)

        // Draw title
        canvas.drawText("Traffic", left + padding, top + padding + titlePaint.textSize, titlePaint)

        // Draw legend items
        var y = top + padding + titlePaint.textSize + itemHeight
        for (item in legendItems) {
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = item.color }
            canvas.drawRect(left + padding, y - boxSize + 4, left + padding + boxSize, y + 4, boxPaint)
            canvas.drawText(item.label, left + padding + textOffset, y, textPaint)
            y += itemHeight
        }
    }
}

/**
 * Map overlay that renders traffic congestion polylines on the osmdroid map.
 */
class TrafficOverlay(private val context: Context) : Overlay() {

    private val trafficSegments = mutableListOf<TrafficSegment>()

    private val paintFree = createPaint(Color.parseColor("#4CAF50"))
    private val paintLight = createPaint(Color.parseColor("#8BC34A"))
    private val paintModerate = createPaint(Color.parseColor("#FF9800"))
    private val paintHeavy = createPaint(Color.parseColor("#F44336"))
    private val paintStandstill = createPaint(Color.parseColor("#8B0000"))

    data class TrafficSegment(
        val points: List<GeoPoint>,
        val congestionLevel: CongestionLevel
    )

    enum class CongestionLevel {
        FREE, LIGHT, MODERATE, HEAVY, STANDSTILL
    }

    private fun createPaint(color: Int): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 12f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            alpha = 180
        }
    }

    fun setTrafficSegments(segments: List<TrafficSegment>) {
        trafficSegments.clear()
        trafficSegments.addAll(segments)
    }

    fun addTrafficSegment(segment: TrafficSegment) {
        trafficSegments.add(segment)
    }

    fun clearTraffic() {
        trafficSegments.clear()
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || trafficSegments.isEmpty()) return

        val projection = mapView.projection

        for (segment in trafficSegments) {
            val paint = when (segment.congestionLevel) {
                CongestionLevel.FREE -> paintFree
                CongestionLevel.LIGHT -> paintLight
                CongestionLevel.MODERATE -> paintModerate
                CongestionLevel.HEAVY -> paintHeavy
                CongestionLevel.STANDSTILL -> paintStandstill
            }

            val path = Path()
            var first = true

            for (point in segment.points) {
                val screenPoint = projection.toPixels(point, null)
                if (first) {
                    path.moveTo(screenPoint.x.toFloat(), screenPoint.y.toFloat())
                    first = false
                } else {
                    path.lineTo(screenPoint.x.toFloat(), screenPoint.y.toFloat())
                }
            }

            canvas.drawPath(path, paint)
        }
    }

    /**
     * Generate simulated traffic data for demonstration.
     * In production, this would fetch from TomTom Traffic API or similar.
     */
    fun generateSimulatedTraffic(center: GeoPoint, radiusMeters: Double = 2000.0) {
        val segments = mutableListOf<TrafficSegment>()
        val levels = CongestionLevel.values()

        // Generate some random road segments with traffic
        for (i in 0 until 8) {
            val angle = (i * 45.0) * Math.PI / 180.0
            val points = mutableListOf<GeoPoint>()

            for (j in 0..5) {
                val dist = (j / 5.0) * radiusMeters
                val lat = center.latitude + (dist / 111000.0) * kotlin.math.cos(angle)
                val lng = center.longitude + (dist / (111000.0 * kotlin.math.cos(center.latitude * Math.PI / 180))) * kotlin.math.sin(angle)
                points.add(GeoPoint(lat, lng))
            }

            segments.add(
                TrafficSegment(
                    points = points,
                    congestionLevel = levels[i % levels.size]
                )
            )
        }

        setTrafficSegments(segments)
    }
}
