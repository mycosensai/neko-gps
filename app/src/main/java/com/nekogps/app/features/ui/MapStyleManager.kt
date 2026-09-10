package com.nekogps.app.features.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.views.MapView

private val Context.mapStyleDataStore by preferencesDataStore(name = "map_style_prefs")

/** Built-in map styles backed by osmdroid tile sources. */
enum class MapStyle(val id: String, val displayName: String) {
    DARK("dark", "Dark"),
    LIGHT("light", "Light"),
    SATELLITE("satellite", "Satellite"),
    HYBRID("hybrid", "Hybrid"),
    TERRAIN("terrain", "Terrain"),
    RETRO("retro", "Retro"),
    CUSTOM("custom", "Custom URL");

    companion object {
        fun fromId(id: String?): MapStyle = values().firstOrNull { it.id == id } ?: DARK
    }
}

/**
 * Applies osmdroid tile sources per selected [MapStyle].
 * Supports custom tile URL templates like https://tile.example.com/{z}/{x}/{y}.png
 */
class MapStyleManager(private val context: Context) {

    companion object {
        private val STYLE_KEY = stringPreferencesKey("map_style")
        private val CUSTOM_URL_KEY = stringPreferencesKey("map_custom_url")
        private const val DEFAULT_CUSTOM_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        private const val TILE_SIZE_PX = 256
        private const val CARTO_DARK_MAX_ZOOM = 19
        private const val OPEN_TOPO_MAX_ZOOM = 17
        private const val STAMEN_TONER_MAX_ZOOM = 18
        private const val CUSTOM_MAX_ZOOM = 19
        private const val MAX_FILE_EXTENSION_LENGTH = 5
    }

    val styleFlow: Flow<MapStyle> = context.mapStyleDataStore.data
        .map { MapStyle.fromId(it[STYLE_KEY]) }

    val customUrlFlow: Flow<String> = context.mapStyleDataStore.data
        .map { it[CUSTOM_URL_KEY] ?: DEFAULT_CUSTOM_URL }

    suspend fun setStyle(style: MapStyle) {
        context.mapStyleDataStore.edit { it[STYLE_KEY] = style.id }
    }

    suspend fun setCustomTileUrl(urlTemplate: String) {
        context.mapStyleDataStore.edit {
            it[CUSTOM_URL_KEY] = urlTemplate
            it[STYLE_KEY] = MapStyle.CUSTOM.id
        }
    }

    suspend fun applyStoredStyle(mapView: MapView) {
        val prefs = context.mapStyleDataStore.data.first()
        applyStyle(mapView, MapStyle.fromId(prefs[STYLE_KEY]), prefs[CUSTOM_URL_KEY])
    }

    fun applyStyle(mapView: MapView, style: MapStyle, customUrl: String? = null) {
        mapView.setTileSource(
            when (style) {
                MapStyle.DARK -> XYTileSource(
                    "CartoDark", 0, CARTO_DARK_MAX_ZOOM, TILE_SIZE_PX, ".png",
                    arrayOf("https://a.basemaps.cartocdn.com/dark_all/")
                )
                MapStyle.LIGHT -> TileSourceFactory.MAPNIK
                MapStyle.SATELLITE -> TileSourceFactory.USGS_SAT
                MapStyle.HYBRID -> TileSourceFactory.USGS_TOPO
                MapStyle.TERRAIN -> XYTileSource(
                    "OpenTopo", 0, OPEN_TOPO_MAX_ZOOM, TILE_SIZE_PX, ".png",
                    arrayOf("https://a.tile.opentopomap.org/")
                )
                MapStyle.RETRO -> XYTileSource(
                    "StamenToner", 0, STAMEN_TONER_MAX_ZOOM, TILE_SIZE_PX, ".png",
                    arrayOf("https://stamen-tiles.a.ssl.fastly.net/toner/")
                )
                MapStyle.CUSTOM -> buildCustomSource(customUrl ?: DEFAULT_CUSTOM_URL)
            }
        )
        // Darken tiles slightly for dark mode readability is handled by theme; keep contrast default.
        mapView.invalidate()
    }

    fun buildCustomSource(urlTemplate: String): XYTileSource {
        // Convert {z}/{x}/{y} template into osmdroid base URL + pattern handling.
        val base = urlTemplate.substringBefore("/{z}").removeSuffix("/") + "/"
        val ext = urlTemplate.substringAfterLast(".", ".png").take(MAX_FILE_EXTENSION_LENGTH).let {
            if (it.startsWith(".")) it else ".png"
        }
        return XYTileSource("Custom", 0, CUSTOM_MAX_ZOOM, TILE_SIZE_PX, ext, arrayOf(base))
    }
}
