package com.nekogps.app.offline

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.ArchiveFileFactory
import org.osmdroid.tileprovider.modules.TileWriter
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.BoundingBox
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Manages offline map tiles using osmdroid's tile caching system.
 * Handles tile downloading, storage management, and offline availability checks.
 */
class OfflineMapManager(context: Context) : OfflineMapCache(context) {

    companion object {
        private const val TAG = "OfflineMapManager"
        private const val MIN_STORAGE_MB = 50L
        private const val TILE_DOWNLOAD_BATCH_SIZE = 20
        private const val BYTES_PER_MEGABYTE = 1_048_576L
        private const val TILE_SERVER_POLITENESS_DELAY_MS = 50L
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
        private const val CACHE_TRIM_RATIO = 0.8
    }

    private val tileWriter = TileWriter()
    private val tileSource: ITileSource = TileSourceFactory.MAPNIK

    /**
     * Information about offline map storage status.
     */
    data class StorageInfo(
        val totalTiles: Int,
        val cacheSizeBytes: Long,
        val cacheSizeFormatted: String,
        val availableStorageBytes: Long,
        val availableStorageFormatted: String
    )

    /**
     * Download progress information.
     */
    data class DownloadProgress(
        val downloaded: Int,
        val total: Int,
        val percentage: Int,
        val currentZoom: Int,
        val isComplete: Boolean = false,
        val error: String? = null
    )

    /**
     * Get current storage information.
     */
    fun getStorageInfo(): StorageInfo {
        val cacheDir = getCacheDir()
        val tiles = cacheDir.walkTopDown().count { it.isFile }
        val cacheSize = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val statFs = StatFs(cacheDir.path)
        val availableBytes = statFs.availableBytes

        return StorageInfo(
            totalTiles = tiles,
            cacheSizeBytes = cacheSize,
            cacheSizeFormatted = formatBytes(cacheSize),
            availableStorageBytes = availableBytes,
            availableStorageFormatted = formatBytes(availableBytes)
        )
    }

    /**
     * Check if there's sufficient storage for offline map downloads.
     * @param requiredMb Required space in megabytes
     * @return True if enough space is available
     */
    fun hasEnoughStorage(requiredMb: Long = MIN_STORAGE_MB): Boolean {
        val statFs = StatFs(getCacheDir().path)
        val availableMb = statFs.availableBytes / BYTES_PER_MEGABYTE
        return availableMb >= requiredMb
    }

    /**
     * Get available storage in megabytes.
     */
    fun getAvailableStorageMb(): Long {
        val statFs = StatFs(getCacheDir().path)
        return statFs.availableBytes / BYTES_PER_MEGABYTE
    }

    /**
     * Download tiles for a region for offline use.
     * @param boundingBox The geographic region to download
     * @param minZoom Minimum zoom level (inclusive)
     * @param maxZoom Maximum zoom level (inclusive)
     * @param onProgress Callback for download progress updates
     * @param onComplete Callback when download finishes
     */
    fun downloadRegion(
        boundingBox: BoundingBox,
        minZoom: Int = 10,
        maxZoom: Int = 17,
        onProgress: (DownloadProgress) -> Unit,
        onComplete: (success: Boolean, error: String?) -> Unit
    ) {
        if (!hasEnoughStorage()) {
            onComplete(false, "Insufficient storage space")
            return
        }

        thread {
            try {
                var downloaded = 0
                val totalTiles = calculateTileCount(boundingBox, minZoom, maxZoom)

                for (zoom in minZoom..maxZoom) {
                    val minTile = getTileNumber(boundingBox.latNorth, boundingBox.lonWest, zoom)
                    val maxTile = getTileNumber(boundingBox.latSouth, boundingBox.lonEast, zoom)

                    for (x in minTile.first..maxTile.first) {
                        for (y in minTile.second..maxTile.second) {
                            try {
                                downloadTile(zoom, x, y)
                                downloaded++

                                if (downloaded % TILE_DOWNLOAD_BATCH_SIZE == 0 || downloaded == totalTiles) {
                                    val progress = DownloadProgress(
                                        downloaded = downloaded,
                                        total = totalTiles,
                                        percentage = (downloaded * 100) / maxOf(totalTiles, 1),
                                        currentZoom = zoom
                                    )
                                    onProgress(progress)
                                }

                                // Small delay to be respectful to tile servers
                                Thread.sleep(TILE_SERVER_POLITENESS_DELAY_MS)
                            } catch (e: IOException) {
                                Log.w(TAG, "Failed to download tile $zoom/$x/$y", e)
                            }
                        }
                    }
                }

                tileWriter.onDetach()

                val finalProgress = DownloadProgress(
                    downloaded = downloaded,
                    total = totalTiles,
                    percentage = 100,
                    currentZoom = maxZoom,
                    isComplete = true
                )
                onProgress(finalProgress)
                onComplete(true, null)

            } catch (e: InterruptedException) {
                Log.w(TAG, "Download failed", e)
                onComplete(false, e.message)
            } catch (e: IOException) {
                Log.w(TAG, "Download failed", e)
                onComplete(false, e.message)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Download failed", e)
                onComplete(false, e.message)
            }
        }
    }

    /**
     * Download a single tile.
     */
    private fun downloadTile(zoom: Int, x: Int, y: Int) {
        val tileUrl = getTileUrl(zoom, x, y)
        val url = URL(tileUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "NekoGPS/1.0")
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val tileFile = File(getCacheDir(), "${tileSource.name()}/$zoom/$x/$y.png")
            tileFile.parentFile?.mkdirs()

            connection.inputStream.use { input ->
                FileOutputStream(tileFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        connection.disconnect()
    }

    /**
     * Check if tiles are available offline for a specific region and zoom.
     */
    fun isRegionAvailableOffline(boundingBox: BoundingBox, zoom: Int): Boolean {
        val minTile = getTileNumber(boundingBox.latNorth, boundingBox.lonWest, zoom)
        val maxTile = getTileNumber(boundingBox.latSouth, boundingBox.lonEast, zoom)

        for (x in minTile.first..maxTile.first) {
            for (y in minTile.second..maxTile.second) {
                val tileFile = File(getCacheDir(), "${tileSource.name()}/$zoom/$x/$y.png")
                if (!tileFile.exists()) return false
            }
        }
        return true
    }

    /**
     * Clear the offline tile cache.
     */
    fun clearCache(): Boolean {
        return try {
            val cacheDir = getCacheDir()
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to clear cache", e)
            false
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Failed to clear cache", e)
            false
        }
    }

    /**
     * Get the size of the tile cache on disk.
     */
    fun getCacheSizeBytes(): Long {
        return getCacheDir().walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * Set the maximum cache size for osmdroid.
     */
    fun setMaxCacheSize(maxBytes: Long) {
        Configuration.getInstance().osmdroidTileCache = getCacheDir()
        Configuration.getInstance().tileFileSystemCacheMaxBytes = maxBytes
        Configuration.getInstance().tileFileSystemCacheTrimBytes = (maxBytes * CACHE_TRIM_RATIO)
            .toLong()
    }

    /**
     * Import offline map archive (MBTiles format).
     */
    fun importArchive(archiveFile: File): Boolean {
        return try {
            val archives = ArchiveFileFactory.getArchiveFile(archiveFile)
            val tileCount = archives.getTileSources().size
            Log.d(TAG, "Imported archive with $tileCount tile sources")
            true
        } catch (e: IOException) {
            Log.w(TAG, "Failed to import archive", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Failed to import archive", e)
            false
        }
    }
}
