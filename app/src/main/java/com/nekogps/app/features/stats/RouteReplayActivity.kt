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
    companion object {
        private const val DEFAULT_MAP_ZOOM = 12.0
        private const val MIN_MAP_ZOOM = 3.0
        private const val MAX_MAP_ZOOM = 19.0
    }

    private lateinit var mapView: MapView
    private var routePoints: List<GeoPoint> = emptyList()
    private var routeName: String = ""
    private val replayPlayer by lazy {
        RouteReplayPlayer(
            mapView = mapView,
            onProgressUpdate = { current, total ->
                findViewById<TextView>(
                    com.nekogps.app.R.id.tvPlaybackProgress
                ).apply {
                    visibility = android.view.View.VISIBLE
                    text = "$current / $total points"
                }
            },
            onPlaybackFinished = {
                findViewById<com.google.android.material.button.MaterialButton>(
                    com.nekogps.app.R.id.btnPlayPause
                ).text = "\u25b6 Play"
            }
        )
    }

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
        mapView.controller.setZoom(DEFAULT_MAP_ZOOM)
        mapView.minZoomLevel = MIN_MAP_ZOOM
        mapView.maxZoomLevel = MAX_MAP_ZOOM
    }

    private fun setupControls() {
        findViewById<com.google.android.material.button.MaterialButton>(
            com.nekogps.app.R.id.btnSelectRoute
        ).setOnClickListener { selectRoute() }
        findViewById<com.google.android.material.button.MaterialButton>(
            com.nekogps.app.R.id.btnPlayPause
        ).setOnClickListener { toggleReplay() }
        findViewById<com.google.android.material.button.MaterialButton>(
            com.nekogps.app.R.id.btnReset
        ).setOnClickListener { resetReplay() }
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
        if (replayPlayer.isReplaying) {
            replayPlayer.stop()
            findViewById<com.google.android.material.button.MaterialButton>(
                com.nekogps.app.R.id.btnPlayPause
            ).text = "\u25b6 Play"
        } else {
            replayPlayer.start(routePoints)
            findViewById<com.google.android.material.button.MaterialButton>(
                com.nekogps.app.R.id.btnPlayPause
            ).text = "\u23f8 Pause"
        }
    }

    private fun resetReplay() {
        replayPlayer.reset(routePoints)
        findViewById<com.google.android.material.button.MaterialButton>(
            com.nekogps.app.R.id.btnPlayPause
        ).text = "\u25b6 Play"
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
        replayPlayer.stop()
        replayPlayer.clear()
    }
}
