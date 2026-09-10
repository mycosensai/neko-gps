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

    /**
     * Input for creating a new bookmark, grouping the fields of [addBookmark].
     */
    data class NewBookmark(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val description: String = "",
        val category: BookmarkCategory = BookmarkCategory.CUSTOM,
        val address: String = ""
    )

    fun getBookmarksByCategory(category: BookmarkCategory): Flow<List<BookmarkEntity>> {
        return dao.getBookmarksByCategory(category)
    }

    suspend fun addBookmark(bookmark: NewBookmark): Long {
        val entity = BookmarkEntity(
            name = bookmark.name,
            description = bookmark.description,
            category = bookmark.category,
            latitude = bookmark.latitude,
            longitude = bookmark.longitude,
            address = bookmark.address
        )
        return dao.insert(entity)
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
