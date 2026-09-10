package com.nekogps.app.features.gamification

import android.content.Context
import com.nekogps.app.features.bookmarks.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * PhotoWaypointManager - allows users to take a photo at a location and attach it
 * to a waypoint or bookmark.
 */
class PhotoWaypointManager(context: Context) {
    data class PhotoWaypointInput(
        val waypointId: Long,
        val bookmarkId: Long,
        val photoPath: String,
        val latitude: Double,
        val longitude: Double,
        val caption: String = ""
    )

    private val dao = AppDatabase.getInstance(context).photoWaypointDao()

    val allPhotos: Flow<List<PhotoWaypointEntity>> = dao.getAllPhotos()

    /**
     * Get all photos associated with a waypoint.
     */
    suspend fun getPhotosForWaypoint(waypointId: Long): List<PhotoWaypointEntity> {
        return dao.getByWaypointId(waypointId)
    }

    /**
     * Get all photos associated with a bookmark.
     */
    suspend fun getPhotosForBookmark(bookmarkId: Long): List<PhotoWaypointEntity> {
        return dao.getByBookmarkId(bookmarkId)
    }

    /**
     * Save a photo waypoint.
     */
    suspend fun addPhotoWaypoint(input: PhotoWaypointInput): Long {
        val photo = PhotoWaypointEntity(
            waypointId = input.waypointId,
            bookmarkId = input.bookmarkId,
            photoPath = input.photoPath,
            latitude = input.latitude,
            longitude = input.longitude,
            caption = input.caption
        )
        return dao.insert(photo)
    }

    /**
     * Remove a photo waypoint.
     */
    suspend fun removePhotoWaypoint(photo: PhotoWaypointEntity) {
        dao.delete(photo)
    }

    /**
     * Remove all photos associated with a waypoint.
     */
    suspend fun removePhotosForWaypoint(waypointId: Long) {
        dao.deleteByWaypointId(waypointId)
    }

    /**
     * Remove all photos associated with a bookmark.
     */
    suspend fun removePhotosForBookmark(bookmarkId: Long) {
        dao.deleteByBookmarkId(bookmarkId)
    }

    /**
     * Get total photo count.
     */
    suspend fun getPhotoCount(): Int {
        return dao.getAllPhotos().first().size
    }
}
