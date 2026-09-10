package com.nekogps.app.offline

import android.content.Context
import java.io.File
import org.osmdroid.util.BoundingBox

/**
 * Shared cache-directory and tile-math foundation for [OfflineMapManager].
 * Holds the pure tile-number conversions and formatting helpers so the manager
 * stays focused on downloading and storage orchestration.
 */
open class OfflineMapCache(private val context: Context) {

    companion object {
        private const val MAP_CACHE_DIR = "osmdroid/tiles"
        private const val LON_OFFSET_DEGREES = 180.0
        private const val FULL_LONGITUDE_RANGE = 360.0
        private const val BYTES_PER_UNIT_STEP = 1024.0
    }

    /**
     * Get the offline map cache directory.
     */
    fun getCacheDir(): File {
        val dir = File(context.getExternalFilesDir(null), MAP_CACHE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Get tile URL from the tile source.
     */
    protected fun getTileUrl(zoom: Int, x: Int, y: Int): String {
        return "https://tile.openstreetmap.org/$zoom/$x/$y.png"
    }

    /**
     * Calculate the number of tiles in a region across zoom levels.
     */
    protected fun calculateTileCount(boundingBox: BoundingBox, minZoom: Int, maxZoom: Int): Int {
        var count = 0
        for (zoom in minZoom..maxZoom) {
            val minTile = getTileNumber(boundingBox.latNorth, boundingBox.lonWest, zoom)
            val maxTile = getTileNumber(boundingBox.latSouth, boundingBox.lonEast, zoom)
            val tilesX = maxTile.first - minTile.first + 1
            val tilesY = maxTile.second - minTile.second + 1
            count += tilesX * tilesY
        }
        return count
    }

    /**
     * Convert lat/lon to tile numbers at a given zoom level.
     */
    protected fun getTileNumber(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
        val x = ((lon + LON_OFFSET_DEGREES) / FULL_LONGITUDE_RANGE * (1 shl zoom)).toInt()
        val mercator = Math.log(Math.tan(Math.toRadians(lat)) + 1.0 / Math.cos(Math.toRadians(lat)))
        val y = ((1.0 - mercator / Math.PI) / 2.0 * (1 shl zoom)).toInt()
        return Pair(x, y)
    }

    /**
     * Format bytes to human-readable string.
     */
    protected fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= BYTES_PER_UNIT_STEP && unitIndex < units.size - 1) {
            size /= BYTES_PER_UNIT_STEP
            unitIndex++
        }
        return String.format(java.util.Locale.getDefault(), "%.2f %s", size, units[unitIndex])
    }
}
