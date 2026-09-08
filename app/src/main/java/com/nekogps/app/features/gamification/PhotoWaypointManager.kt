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
    suspend fun addPhotoWaypoint(
        waypointId: Long,
        bookmarkId: Long,
        photoPath: String,
        latitude: Double,
        longitude: Double,
        caption: String = ""
    ): Long {
        val photo = PhotoWaypointEntity(
            waypointId = waypointId,
            bookmarkId = bookmarkId,
            photoPath = photoPath,
            latitude = latitude,
            longitude = longitude,
            caption = caption
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
