package com.nekogps.app.features.bookmarks

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: BookmarkCategory = BookmarkCategory.CUSTOM,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class BookmarkCategory(val displayName: String, val emoji: String) {
    HOME("Home", "🏠"),
    WORK("Work", "💼"),
    FAVORITE("Favorite", "⭐"),
    CUSTOM("Custom", "📍")
}
