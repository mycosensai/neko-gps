package com.nekogps.app.features.stats

import android.os.Bundle
import android.view.View
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.nekogps.app.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.util.GeoPoint

/**
 * RouteReplayActivity - animates saved route playback on osmdroid map.
 */
class RouteReplayActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private var replayPolyline: Polyline? = null
    private var replayMarker: Marker? = null
    private var isReplaying = false
    private var currentPointIndex = 0
    private var routePoints: List<GeoPoint> = emptyList()
    private var routeName: String = ""
    private var handler: Handler? = null
    private lateinit var replayRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.nekogps.app.R.layout.activity_route_replay)

        Configuration.getInstance().userAgentValue = packageName
        mapView = findViewById(com.nekogps.app.R.id.mapReplay)
        setupMap()
        setupControls()
        loadRouteList()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(12.0)
        mapView.minZoomLevel = 3.0
        mapView.maxZoomLevel = 19.0
    }

    private fun setupControls() {
        findViewById<com.google.android.material.button.MaterialButton>(com.nekogps.app.R.id.btnSelectRoute).setOnClickListener { selectRoute() }
        findViewById<com.google.android.material.button.MaterialButton>(com.nekogps.app.R.id.btnPlayPause).setOnClickListener { toggleReplay() }
        findViewById<com.google.android.material.button.MaterialButton>(com.nekogps.app.R.id.btnReset).setOnClickListener { resetReplay() }
    }

    private fun selectRoute() {
        val routeHistory = RouteHistoryManager(this)
        val routes = routeHistory.getAllRouteNames()

        if (routes.isEmpty()) {
            
            return
        }

        routeName = routes[0]
        findViewById<TextView>(com.nekogps.app.R.id.tvRouteName).text = routeName
        routePoints = routeHistory.getRoute(routeName)?.map { GeoPoint(it.latitude, it.longitude) } ?: emptyList()

        if (routePoints.isNotEmpty()) {
            val midLat = routePoints.map { it.latitude }.average()
            val midLon = routePoints.map { it.longitude }.average()
            mapView.controller.animateTo(GeoPoint(midLat, midLon))
            
        }
    }

    private fun toggleReplay() {
        if (routePoints.isEmpty()) {
            
            return
        }
        isReplaying = !isReplaying
        if (isReplaying) {
            startReplay()
            findViewById<com.google.android.material.button.MaterialButton>(com.nekogps.app.R.id.btnPlayPause).text = "\u23f8 Pause"
        } else {
            stopReplay()
            findViewById<com.google.android.material.button.MaterialButton>(com.nekogps.app.R.id.btnPlayPause).text = "\u25b6 Play"
        }
    }

    private fun resetReplay() {
        stopReplay()
        isReplaying = false
        currentPointIndex = 0
        findViewById<com.google.android.material.button.MaterialButton>(com.nekogps.app.R.id.btnPlayPause).text = "\u25b6 Play"
        clearReplayMap()
        if (routePoints.isNotEmpty()) {
            val startPoint = routePoints[0]
            replayMarker = Marker(mapView).apply {
                position = GeoPoint(startPoint.latitude, startPoint.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Start"
            }
            mapView.overlays.add(replayMarker)
            mapView.controller.animateTo(GeoPoint(startPoint.latitude, startPoint.longitude))
            mapView.invalidate()
        }
    }

    private fun startReplay() {
        currentPointIndex = 0
        handler = Handler(Looper.getMainLooper())
        replayRunnable = Runnable {
            if (currentPointIndex < routePoints.size && isReplaying) {
                val point = routePoints[currentPointIndex]
                val geoPoint = GeoPoint(point.latitude, point.longitude)

                if (replayPolyline == null) {
                    replayPolyline = Polyline(mapView).apply {
                        color = android.graphics.Color.parseColor("#cbb7fb")
                        width = 8f
                    }
                    mapView.overlays.add(replayPolyline)
                }
                val points = ArrayList<GeoPoint>()
                for (i in 0..currentPointIndex) {
                    points.add(GeoPoint(routePoints[i].latitude, routePoints[i].longitude))
                }
                mapView.overlays.remove(replayPolyline)
                val newPolyline = Polyline().apply {
                    setPoints(routePoints.subList(0, currentPointIndex + 1).map { GeoPoint(it.latitude, it.longitude) })
                    color = android.graphics.Color.parseColor("#cbb7fb")
                    width = 8f
                }
                mapView.overlays.add(newPolyline)
                replayPolyline = newPolyline

                if (replayMarker == null) {
                    replayMarker = Marker(mapView).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Current Location"
                    }
                    mapView.overlays.add(replayMarker)
                }
                replayMarker?.position = geoPoint
                mapView.controller.animateTo(geoPoint)

                findViewById<TextView>(com.nekogps.app.R.id.tvPlaybackProgress).apply {
                    visibility = android.view.View.VISIBLE
                    text = "${currentPointIndex + 1} / ${routePoints.size} points"
                }

                currentPointIndex++
                handler?.postDelayed(replayRunnable, 500)
            } else {
                isReplaying = false
                findViewById<com.google.android.material.button.MaterialButton>(com.nekogps.app.R.id.btnPlayPause).text = "\u25b6 Play"
            }
        }
        handler?.post(replayRunnable)
    }

    private fun stopReplay() {
        handler?.removeCallbacks(replayRunnable)
    }

    private fun clearReplayMap() {
        replayPolyline?.let { mapView.overlays.remove(it) }
        replayPolyline = null
        replayMarker?.let { mapView.overlays.remove(it) }
        replayMarker = null
        mapView.invalidate()
    }

    private fun loadRouteList() {
        val routeHistory = RouteHistoryManager(this)
        val routes = routeHistory.getAllRouteNames()
        if (routes.isNotEmpty()) {
            routeName = routes[0]
            findViewById<TextView>(com.nekogps.app.R.id.tvRouteName).text = routeName
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopReplay()
        clearReplayMap()
    }
}
