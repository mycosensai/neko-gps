package com.nekogps.app.features.bookmarks

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * BookmarkManager - manages saved location bookmarks using Room database.
 * Supports categories: Home, Work, Favorite, Custom.
 */
class BookmarkManager(context: Context) {
    private val dao = AppDatabase.getInstance(context).bookmarkDao()

    val allBookmarks: Flow<List<BookmarkEntity>> = dao.getAllBookmarks()

    fun getBookmarksByCategory(category: BookmarkCategory): Flow<List<BookmarkEntity>> {
        return dao.getBookmarksByCategory(category)
    }

    suspend fun addBookmark(
        name: String,
        description: String = "",
        category: BookmarkCategory = BookmarkCategory.CUSTOM,
        latitude: Double,
        longitude: Double,
        address: String = ""
    ): Long {
        val bookmark = BookmarkEntity(
            name = name,
            description = description,
            category = category,
            latitude = latitude,
            longitude = longitude,
            address = address
        )
        return dao.insert(bookmark)
    }

    suspend fun updateBookmark(bookmark: BookmarkEntity) {
        dao.update(bookmark)
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) {
        dao.delete(bookmark)
    }

    suspend fun deleteBookmarkById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun getBookmarkById(id: Long): BookmarkEntity? {
        return dao.getBookmarkById(id)
    }

    suspend fun searchBookmarks(query: String): List<BookmarkEntity> {
        return dao.searchBookmarks(query)
    }

    suspend fun getBookmarkCount(): Int {
        return dao.getCount()
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }
}
